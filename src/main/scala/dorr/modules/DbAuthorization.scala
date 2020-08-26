package dorr.modules

import cats.Functor
import dorr.http.To
import dorr.modules.AuthMeta.VkOAuth
import dorr.modules.defs.SessionData
import dorr.modules.dsl.AuthManager
import ru.tinkoff.tschema.finagle.Authorization.OAuth2
import ru.tinkoff.tschema.finagle.{Authorization, LiftHttp, Rejection, Routed}
import ru.tinkoff.tschema.utils.Provision
import tofu.syntax.monadic._

object AuthMeta {
  type VkOAuth[H[_]] = Authorization[OAuth2, H, To, SessionData]
  type FromProv[H[_]] = Provision[H, SessionData]
}

class DbAuthorization[H[_]: Routed, F[_]: AuthManager: Functor](implicit Lift: LiftHttp[H, F]) extends VkOAuth[H] {
  def reject[A]: H[A] = Routed[H].reject[A](Rejection.unauthorized)

  override def apply(s: Option[SessionData]): H[To] = s match {
    case Some(sessionData) =>
      Lift(AuthManager[F].authorize(sessionData) map To)
    case None =>
      reject
  }
}