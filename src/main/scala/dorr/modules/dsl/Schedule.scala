package dorr.modules.dsl

import java.time.LocalDateTime

trait Schedule[F[_]] {
  def nextDate: F[LocalDateTime]
}

object Schedule {
  def apply[F[_]](implicit ev: Schedule[F]): Schedule[F] = ev
}
