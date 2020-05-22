package dorr.contrib

import java.net.URI

import cats.{Applicative, Monad}
import com.twitter.finagle.http.{Cookie, Response, Status}
import ru.tinkoff.tschema.finagle.{Completing, LiftHttp}
import tofu.syntax.monadic._

class oAuthRedirect

object oAuthRedirect {
  type RedirectParams = (URI, String)

  //TODO make cookies secure
  implicit def redirect[H[_]: Monad, F[_]: Applicative](
    implicit LH: LiftHttp[H, F]
  ): Completing[H, oAuthRedirect, F[RedirectParams]] = {
    p =>
      val redirectResponse = p map { case (uri, state) =>
        val response = Response(Status.MovedPermanently)
        response.headerMap.set("Location", uri.toString)
        response.addCookie(new Cookie("state", state.trim))
        response
      }
      LH(redirectResponse)
  }
}
