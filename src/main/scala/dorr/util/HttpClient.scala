package dorr.util

trait HttpClient[F[_]] {
  def https(host: String, path: String, params: (String, String)*): F[String]
}

object HttpClient {
  def apply[F[_]](implicit ev: HttpClient[F]): HttpClient[F] = ev
}