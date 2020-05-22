package dorr.util

import cats.Functor
import com.twitter.finagle.{Http, Service, http}
import com.twitter.util.Future
import tofu.lift.Lift
import tofu.syntax.monadic._

class FinagleHttpClient[F[_]: Lift[Future, *[_]]: Functor] extends HttpClient[F] {
  override def https(host: String, path: String, params: (String, String)*): F[String] = {
    Lift[Future, F].lift {
      val service: Service[http.Request, http.Response] =
        Http.client.withTransport
          .tls(host)
          .newService(s"$host:443")

      val request = http.Request(path, params: _*)

      service(request)
    } map (_.contentString)
  }
}
