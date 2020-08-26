package dorr.contrib.tschema

import cats.Functor
import com.twitter.finagle.http.{Cookie, Response}
import dorr.contrib.tschema.Redirect.status
import dorr.modules.defs.SessionData
import ru.tinkoff.tschema.finagle.{Completing, LiftHttp}
import ru.tinkoff.tschema.swagger.MkSwagger
import ru.tinkoff.tschema.typeDSL.Complete
import tofu.syntax.monadic._

class Authenticate

object Authenticate {
  val csrfBearer = "csrf_token"
  val sessionId = "session_id"

  implicit def authenticate[H[_], F[_] : Functor](
    implicit Lift: LiftHttp[H, F]
  ): Completing[H, Authenticate, F[SessionData]] = { arg =>
    Lift(arg map { sessionData =>
      val response = Response(status)

      response.addCookie(
        new Cookie(csrfBearer, sessionData.csrfMac)
      )

      response.addCookie(
        new Cookie(sessionId, sessionData.sessionId)
      )

      response
    })
  }

  implicit val swaggerResult: MkSwagger[Complete[Authenticate]] =
    MkSwagger.empty //TODO
}
