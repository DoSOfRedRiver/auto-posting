package dorr.http


import cats.Monad
import dorr.util.storage.Prefixed.prefixedStorage
import dorr.util.storage.Storage
import dorr.util.syntax.optionF._
import ru.tinkoff.tschema.finagle.Authorization.Bearer
import ru.tinkoff.tschema.finagle.{Authorization, LiftHttp, Rejection, Routed}

class DbAuthorization[H[_]: Monad: Routed, F[_]: Storage[*[_], AuthData]](implicit Lift: LiftHttp[H, F]) extends Authorization[Bearer, H, AuthData] {
  val authDataStorage = prefixedStorage(AuthData.tablePrefix)
  val bearerPattern = "Bearer "

  override def apply(s: Option[String]): H[AuthData] = s match {
    case Some(token) if token.startsWith(bearerPattern) =>
      val secret = token.substring(bearerPattern.length)
      Lift(authDataStorage.get(secret)) getOrElseF Routed[H].reject(Rejection.unauthorized)
    case None =>
      Routed[H].reject(Rejection.unauthorized)
  }
}
