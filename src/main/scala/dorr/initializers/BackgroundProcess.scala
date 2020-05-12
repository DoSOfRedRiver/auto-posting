package dorr.initializers

import cats.effect.Fiber

trait BackgroundProcessToken
object BackgroundProcessToken extends BackgroundProcessToken

trait BackgroundProcess[F[_]] {
  def start: F[Fiber[F, Unit]]
}