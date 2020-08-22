package dorr.contrib.tofu

import cats.effect.syntax.bracket._
import cats.effect.{Async, ContextShift}
import com.twitter.util.{Future, Return, Throw}

trait Execute[F[_], G[_]] {
  def deferExecuteAction[A](fa: => F[A]): G[A]
}

object Execute {

  implicit def asyncExecuteTwitter[G[_] : Async](implicit cs: ContextShift[G]): Execute[Future, G] =
    new Execute[Future, G] {
      override def deferExecuteAction[A](fa: => Future[A]): G[A] = {
        Async[G].defer {
          Async[G].async[A](cb => fa.respond {
            case Throw(e) =>
              cb(Left(e))
            case Return(r) =>
              cb(Right(r))
          }).guarantee(cs.shift)
        }
      }
    }
}
