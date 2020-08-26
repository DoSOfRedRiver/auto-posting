package dorr.modules.impl

import java.time.Instant

import cats.Monad
import dorr.modules.defs.{IdData, SessionData, Sessions}
import dorr.modules.dsl.AuthManager
import dorr.util.storage.Prefixed.prefixedStorage
import dorr.util.storage.Storage
import dorr.util.{Crypto, Time}
import tofu.BracketThrow
import tofu.generate.GenUUID
import tofu.syntax.monadic._
import tofu.syntax.raise._
import tsec.common._

class GohAuthManager[F[_]: GenUUID: Monad: Crypto: Time: BracketThrow: Storage[*[_], IdData]](key: Crypto[F]#Key,storage: Storage[F, String]) extends AuthManager[F] {
  val authDataStorage = prefixedStorage(IdData.tablePrefix)
  val sessionStorage = prefixedStorage(Sessions.sessionsPrefix)(storage)

  val lifetime = 1488

  override def authenticate(data: IdData): F[SessionData] = {
    val userId = s"${data.provider}-${data.userId}"

    for {
      acc           <- authDataStorage.get(userId)
      _             <- createAccount(data, userId) whenA acc.isEmpty
      sessionId     <- createSession(userId)
      (mac, plain)  <- generateCsrfToken(sessionId)
    } yield SessionData(sessionId, mac, plain)
  }

  //TODO check privilege
  //TODO summon some kind of Session object into implicit scope
  def authorize(sessionData: SessionData): F[String] = {
    checkCsrfToken(
      mac = sessionData.csrfMac,
      plain = sessionData.csrfPlain,
      sessionId = sessionData.sessionId
    ).ifM(unit, new IllegalArgumentException("Unauthorized").raise).as(sessionData.sessionId)
  }

  //TODO create profile?
  def createAccount(data: IdData, userId: String): F[Unit] = authDataStorage.put(userId, data)

  def createSession(userId: String): F[String] =
    GenUUID.randomString[F] flatTap (sessionStorage.put(userId, _))

  def generateCsrfToken(sessionId: String): F[(String, String)] =
    for {
      instant <- Time[F].instant
      eol     =  instant.plusMillis(lifetime).getEpochSecond
      plain   =  s"$sessionId//$eol"
      mac     <- Crypto[F].hmac(key, plain.utf8Bytes)
    } yield (new String(mac), plain)

  def checkCsrfToken(mac: String, plain: String, sessionId: String): F[Boolean] = {
    val (csrfSession, eol) = parseCsrf(plain)

    for {
      verified  <- Crypto[F].verify(key, mac.getBytes, plain.getBytes)
      alive     <- Time[F].instant map eol.isAfter
    } yield verified && alive && (csrfSession == sessionId)
  }

  def parseCsrf(csrfToken: String): (String, Instant) = {
    val (sessionId, s"//$timestamp") = {
      val ind = csrfToken.lastIndexOf("//")
      csrfToken.splitAt(ind)
    }
    (sessionId, Instant.ofEpochSecond(timestamp.toLong))
  }
}
