package dorr.initializers

import cats.effect.{Fiber, Sync}
import cats.instances.list._
import cats.syntax.applicativeError._
import cats.syntax.foldable._
import com.twitter.finagle
import com.twitter.finagle.http.{Response, Status}
import dorr.Configuration.Config
import logstage.LogIO
import ru.tinkoff.tschema.finagle.{RoutedPlus, RunHttp}
import tofu.lift.Lift
import tofu.syntax.monadic._
import tofu.syntax.start._
import tofu.{BracketThrow, Start}

class HttpServerInit[H[_]: RoutedPlus: BracketThrow, F[_]: LogIO: Sync: Start](routes: Set[H[Response]], conf: Config, L: Lift[F, H], R: RunHttp[H, F]) extends BackgroundProcess[F] {
  override def start: F[Fiber[F, Unit]] = {
    val server = for {
      service <- R.run(routes.toList.foldK)
      server  <- Sync[F].delay(finagle.Http.serve(conf.httpAddr, service))
      _       <- LogIO[F].info(s"HTTP server started at ${server.boundAddress}")
      _       <- Start[F].never[Void]
    } yield ()

    server.start
  }
}