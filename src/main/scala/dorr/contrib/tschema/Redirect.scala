package dorr.contrib.tschema

import java.net.URI

import cats.Functor
import com.twitter.finagle.http.{Response, Status}
import dorr.contrib.tschema.Swagger.MovedPermanently
import ru.tinkoff.tschema.finagle.{Completing, LiftHttp}
import ru.tinkoff.tschema.swagger.{MkSwagger, OpenApiOp, OpenApiResponses}
import ru.tinkoff.tschema.typeDSL.Complete
import tofu.syntax.monadic._

import scala.collection.immutable.TreeMap

class Redirect

object Redirect {
  val status: Status = Status.MovedPermanently

  implicit def redirect[H[_], F[_] : Functor](
    implicit LH: LiftHttp[H, F]
  ): Completing[H, Redirect, F[URI]] = {
    c =>
      LH(c map { uri =>
        val response = Response(status)
        response.headerMap.set("Location", uri.toString)
        response
      })
  }

  implicit val swaggerResult: MkSwagger[Complete[Redirect]] =
    MkSwagger.single(
      OpenApiOp(responses = OpenApiResponses(
        codes = Map(301 -> MovedPermanently)
      )),
      TreeMap.empty
    )
}
