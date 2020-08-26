package dorr.testutils

import java.time.{Instant, LocalDateTime, ZoneOffset}

import cats.Applicative
import cats.syntax.applicative._
import dorr.util.Time

class TimeDummy[F[_]: Applicative](localDateTime: LocalDateTime) extends Time[F] {
  override def now: F[LocalDateTime] = localDateTime.pure[F]

  override def instant: F[Instant] = localDateTime.toInstant(ZoneOffset.UTC).pure[F]
}
