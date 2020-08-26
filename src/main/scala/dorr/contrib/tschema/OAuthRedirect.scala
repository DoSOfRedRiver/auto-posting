package dorr.contrib.tschema

import java.net.URI

import cats.{Applicative, Monad}
import com.twitter.finagle.http.{Cookie, Response, Status}
import ru.tinkoff.tschema.finagle.{Completing, LiftHttp}
import ru.tinkoff.tschema.swagger.MkSwagger
import ru.tinkoff.tschema.typeDSL.Complete
import tofu.syntax.monadic._

class OAuthRedirect

object OAuthRedirect {
  type RedirectParams = (URI, String)

  val status = Status.MovedPermanently

  //TODO make cookies secure
  implicit def redirect[H[_]: Monad, F[_]: Applicative](
    implicit Lift: LiftHttp[H, F]
  ): Completing[H, OAuthRedirect, F[RedirectParams]] = {
    p =>
      val redirectResponse = p map { case (uri, state) =>
        val response = Response(status)
        response.headerMap.set("Location", uri.toString)
        response.addCookie(new Cookie("state", state.trim))
        response
      }
      Lift(redirectResponse)
  }

  implicit val swaggerResult: MkSwagger[Complete[OAuthRedirect]] =
    Redirect.swaggerResult.asInstanceOf[MkSwagger[Complete[OAuthRedirect]]]
}
