package dorr.modules.impl

import java.time.Instant

import cats.Monad
import dorr.Configuration.Config
import dorr.modules.defs.{IdData, Profile, SessionData}
import dorr.modules.dsl.AuthManager
import dorr.util.Time
import dorr.util.storage.Prefixed.prefixedStorage
import dorr.util.storage.Storage
import tofu.BracketThrow
import tofu.generate.GenUUID
import tofu.syntax.monadic._
import tofu.syntax.raise._


class GohAuthManager[F[_]: GenUUID: Monad: Time: BracketThrow: Storage[*[_], IdData]: Storage[*[_], Profile]](storage: Storage[F, String], cfg: Config) extends AuthManager[F] {
  val authDataStorage = prefixedStorage[F, IdData](IdData.tablePrefix)
  val profileStorage = prefixedStorage[F, Profile](Profile.tablePrefix)
  val sessionStorage = prefixedStorage("session")(storage)
  val csrfStorage = prefixedStorage("csrf")(storage)

  override def authenticate(data: IdData): F[SessionData] = {
    val userId = s"${data.provider}-${data.externalId}"

    for {
      acc       <- authDataStorage.get(userId)
      _         <- createAccount(data, userId) whenA acc.isEmpty
      sessionId <- createSession(userId)
      csrfToken <- generateCsrfToken(sessionId)
    } yield SessionData(sessionId, csrfToken)
  }

  //TODO check privilege
  //TODO summon some kind of Session object into implicit scope
  def authorize(sessionData: SessionData): F[String] = {
    checkCsrfToken(
      sessionData.csrfToken,
      sessionData.sessionId
    ).ifM(unit, new IllegalArgumentException("Unauthorized").raise).as(sessionData.sessionId)
  }

  //TODO create profile?
  def createAccount(data: IdData, userId: String): F[Unit] =
    for {
      _ <- authDataStorage.put(userId, data)
      _ <- profileStorage.put("alex", Profile("My name is Alex", "alex"))
    } yield ()

  def createSession(userId: String): F[String] =
    GenUUID.randomString[F] flatTap (sessionStorage.put(_, userId))

  def generateCsrfToken(sessionId: String): F[String] =
    for {
      instant <- Time[F].instant
      token   <- GenUUID.randomString[F]
      eol     =  instant.plusMillis(cfg.security.csrfLifetime).getEpochSecond
      plain   =  s"$sessionId//$eol"
      _       <- csrfStorage.put(token, plain)
    } yield token

  def checkCsrfToken(csrfToken: String, sessionId: String): F[Boolean] = {
    csrfStorage.get(csrfToken) >>= {
      case Some(parseCsrf(csrfId, eol)) =>
        for {
          isBefore <- Time[F].instant map eol.isAfter
        } yield isBefore && csrfId == sessionId
      case None =>
        false.pure
    }
  }

  object parseCsrf {
    def unapply(token: String): Option[(String, Instant)] = {
      val ind = token.lastIndexOf("//")

      if (ind != -1) {
        val (sessionId, s"//$timestamp") = token.splitAt(ind)
        Some(sessionId, Instant.ofEpochSecond(timestamp.toLong))
      } else None
    }
  }
}
