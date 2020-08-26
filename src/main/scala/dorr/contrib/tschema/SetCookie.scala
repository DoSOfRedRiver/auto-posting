package dorr.contrib.tschema

import cats.Functor
import com.twitter.finagle.http.{Cookie, Response, Status}
import ru.tinkoff.tschema.finagle.{Completing, LiftHttp}
import tofu.syntax.monadic._

class SetCookie

object SetCookie {
  implicit def setCookie[H[_], F[_] : Functor](
    implicit Lift: LiftHttp[H, F]
  ): Completing[H, SetCookie, F[Cookie]] = {
    c =>
      Lift(c map { cookie =>
        val response = Response(Status.Ok)
        response.addCookie(cookie)
        response
      })
  }
}
