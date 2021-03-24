package dorr.http

import cats.Applicative
import cats.syntax.applicative._

trait StatusHandler[F[_]] {
  implicit val applicative: Applicative[F]

  def status: F[String] = "Alive".pure
}
