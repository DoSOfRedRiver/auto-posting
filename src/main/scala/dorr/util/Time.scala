package dorr.util

import java.time.{Instant, LocalDateTime}

trait Time[F[_]] {
  def now: F[LocalDateTime]

  def instant: F[Instant]
}

object Time {
  def apply[F[_]: Time]: Time[F] = implicitly
}
