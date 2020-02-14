package dorr.contib

import cats.Functor

object Context {
  def create[F[_]: Functor, A](ctx: F[A]) = {
    new tofu.Context[F] {
      override type Ctx = A
      override def functor: Functor[F] = Functor[F]
      override def context: F[A] = ctx
    }
  }
}
