package dorr.modules

import cats.Functor
import cats.instances.option._
import dorr.contrib.tschema.Authenticate
import dorr.modules.defs.SessionData
import ru.tinkoff.tschema.finagle.Routed
import ru.tinkoff.tschema.utils.Provision
import tofu.syntax.monadic._

class OAuthProvision[F[_]: Routed: Functor] extends Provision[F, SessionData] {
  val bearerHeader = "CsrfBearer"

  override def provide(): F[Option[SessionData]] = {
    Routed[F].request map { req =>
      val csrfBearer = req.headerMap.get(bearerHeader)
      val sessionId = req.cookies.get(Authenticate.sessionId).map(_.value)

      (sessionId, csrfBearer) mapN SessionData.apply
    }
  }
}
