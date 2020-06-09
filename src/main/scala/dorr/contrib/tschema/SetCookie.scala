package dorr.contrib.tschema

import cats.Functor
import com.twitter.finagle.http.{Cookie, Response, Status}
import ru.tinkoff.tschema.finagle.{Completing, LiftHttp}
import tofu.syntax.monadic._

class SetCookie

object SetCookie {
  implicit def redirect[H[_], F[_] : Functor](
    implicit LH: LiftHttp[H, F]
  ): Completing[H, SetCookie, F[Cookie]] = {
    c =>
      LH(c map { cookie =>
        val response = Response(Status.Ok)
        response.addCookie(cookie)
        response
      })
  }
}
