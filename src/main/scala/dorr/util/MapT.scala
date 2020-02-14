package dorr.util

import cats.Functor
import cats.data.OptionT

trait MapT[F[_], G[_]] {
  def mapT[A, B](fa: F[G[A]])(f: A => B): F[G[B]]
}

trait MapTInstances {
  implicit def optionMapT[F[_]: Functor] = new MapT[F, Option] {
    override def mapT[A, B](fa: F[Option[A]])(f: A => B) = OptionT(fa).map(f).value
  }
}

trait MapTSyntax {
  implicit class MapTOps[F[_], G[_], A](fga: F[G[A]])(implicit MT: MapT[F, G]) {
    def mapT[B](f: A => B): F[G[B]] = MT.mapT(fga)(f)
  }
}