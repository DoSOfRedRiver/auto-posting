package dorr.testutils

import java.time.LocalDateTime

import cats.Applicative
import cats.syntax.applicative._
import dorr.util.Time

class TimeDummy[F[_]: Applicative](localDateTime: LocalDateTime) extends Time[F] {
  override def now: F[LocalDateTime] = localDateTime.pure[F]
}
