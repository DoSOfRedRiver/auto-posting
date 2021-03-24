package dorr.contrib.tschema

import cats.Functor
import com.twitter.finagle.http.cookie.SameSite
import com.twitter.finagle.http.{Cookie, Response}
import dorr.contrib.tschema.Redirect.status
import dorr.modules.defs.SessionData
import ru.tinkoff.tschema.finagle.{Completing, LiftHttp}
import ru.tinkoff.tschema.swagger.{MkSwagger, SwaggerPrimitive}
import ru.tinkoff.tschema.typeDSL.Complete
import tofu.syntax.monadic._
import com.twitter.conversions.DurationOps._

class Authenticate

object Authenticate {
  val sessionId = "session_id"

  implicit def authenticate[H[_], F[_] : Functor](
    implicit Lift: LiftHttp[H, F]
  ): Completing[H, Authenticate, F[SessionData]] = { arg =>
    Lift(arg map { sessionData =>
      val response = Response(status)

      val session = new Cookie(
        sessionId,
        sessionData.sessionId,
        sameSite = SameSite.Lax,
        httpOnly = true,
        maxAge = Some(30.days)
      )

      response.addCookie(session)
      response.contentString = sessionData.csrfToken

      response
    })
  }

  implicit val swaggerResult: MkSwagger[Complete[Authenticate]] =
    MkSwagger.summon[Complete[String]]
      .addDescribedResponse(200, SwaggerPrimitive.string.withMediaType("text/plain"))
      .as[Complete[Authenticate]]
}
