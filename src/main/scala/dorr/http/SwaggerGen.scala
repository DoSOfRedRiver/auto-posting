package dorr.http

import java.util.Locale

import cats.Monad
import cats.syntax.semigroupk._
import com.twitter.finagle.http.Response
import dorr.contrib.tschema.SwaggerIndex
import io.circe.Printer
import monix.eval.Task
import monix.execution.Scheduler
import ru.tinkoff.tschema.finagle.envRouting.{Rejected, TaskRouting}
import ru.tinkoff.tschema.finagle.util.message
import ru.tinkoff.tschema.finagle.{Rejection, Routed, RoutedPlus}
import ru.tinkoff.tschema.swagger.{MkSwagger, OpenApiInfo, PathDescription}
import tofu.env.Env
import tofu.syntax.monadic._

class SwaggerGen[H[_]: RoutedPlus: Monad](rts: Routes) {
  import rts._

  implicit val printer: Printer = Printer.spaces2.copy(dropNullValues = true)

  import cats.instances.list._
  import cats.syntax.foldable._
  import io.circe.syntax._

  val resources = Scheduler.io()

  val routes = List(MkSwagger(auth), MkSwagger(upload), MkSwagger(status)).combineAll
  val descriptions =
    PathDescription.utf8I18n("swagger", Locale.forLanguageTag("ru"))


  val json     = routes.describe(descriptions).make(OpenApiInfo()).asJson.printWith(printer)
  val response = message.jsonResponse(json)

  val swaggerJson: H[Response] =
    Routed.checkPath[H, Response]("/swagger.json", response.pure[H])

  val swaggerHttp: H[Response] = {
    val response = message.stringResponse(SwaggerIndex.index.render)
    response.setContentType("text/html(UTF-8)")
    Routed.checkPath[H, Response]("/swagger", response.pure[H])
  }

  val swaggerResources: H[Response] =
    Routed.path[H].map(_.toString).flatMap {
      case s if s.startsWith("/webjars") => resource("/META-INF/resources" + s)
      case _                             => Routed.reject[H, Response](Rejection.notFound)
    }

  private def resource(name: String): H[Response] =
    Env.fromTask[TaskRouting, Response](
      Task.delay {
        val BufSize  = 1024
        val response = Response()
        val stream   = getClass.getResourceAsStream(name)
        val arr      = Array.ofDim[Byte](BufSize)
        def readAll(): Unit =
          stream.read(arr) match {
            case BufSize =>
              response.write(arr)
              readAll()
            case size if size > 0 =>
              response.write(arr.slice(0, size))
              readAll()
            case _ =>
          }
        readAll()
        response
      }.executeOn(resources)
        .onErrorHandleWith(_ => Task.raiseError(Rejected(Rejection.notFound)))).asInstanceOf[H[Response]] //TODO

  val route: H[Response] = swaggerResources <+> swaggerHttp <+> swaggerJson
}
