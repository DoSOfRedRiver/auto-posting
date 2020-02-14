package dorr.modules.dsl

trait Schedule[F[_]] {
  def nextDate: F[Long]
}

object Schedule {
  def apply[F[_]](implicit ev: Schedule[F]): Schedule[F] = ev
}
