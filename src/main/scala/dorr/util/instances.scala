package dorr.util

import java.time.LocalDateTime

import cats.effect.Sync

object instances {
  implicit def syncTime[F[_]: Sync]: Time[F] = new Time[F] {
    override def now: F[LocalDateTime] = Sync[F].delay(LocalDateTime.now())
  }
}
