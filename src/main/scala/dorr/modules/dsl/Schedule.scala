package dorr.modules.dsl

import java.time.LocalDateTime

trait Schedule[F[_]] {
  def nextDates(n: Int): F[List[LocalDateTime]]
}

object Schedule {
  def apply[F[_]](implicit ev: Schedule[F]): Schedule[F] = ev
}
