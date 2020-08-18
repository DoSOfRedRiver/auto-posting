package dorr.util

import cats.Monad
import tofu.syntax.monadic._

trait OptionFSyntax {
  implicit class OptionFOps[F[_]: Monad, A](foa: F[Option[A]]) {
    def getOrElseM(a: A): F[A] = foa.map(_.getOrElse(a))
    def getOrElseF(fa: F[A]): F[A] = foa flatMap {
      case Some(value) =>
        value.pure[F]
      case None =>
        fa
    }
  }
}
