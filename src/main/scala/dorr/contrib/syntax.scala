package dorr.contrib

import tofu.Context

object syntax {
  class ContextC[Has[_[_]]] {
    def apply[F[_], A](implicit ev: Has[F], ev1: Has[F] <:< Context.Aux[F, A]): F[A] =
      ev.context
  }

  def hasContext[Has[_[_]]] = new ContextC[Has]
}
