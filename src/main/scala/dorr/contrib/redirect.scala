package dorr.contrib

import java.net.URI

import cats.Functor
import com.twitter.finagle.http.{Cookie, Response, Status}
import ru.tinkoff.tschema.finagle.{Completing, LiftHttp}
import tofu.syntax.monadic._

class redirect

object redirect{
  implicit def redirect[H[_], F[_] : Functor](
    implicit LH: LiftHttp[H, F]
  ): Completing[H, redirect, F[URI]] = {
    c =>
      LH(c map { uri =>
        val response = Response(Status.MovedPermanently)
        response.headerMap.set("Location", uri.toString)
        response
      })
  }
}
