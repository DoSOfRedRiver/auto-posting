package dorr.util

import java.time.LocalDateTime

trait Time[F[_]] {
  def now: F[LocalDateTime]
}

object Time {
  def apply[F[_]: Time]: Time[F] = implicitly
}
