package dorr.util.storage

trait Storage[F[_], A] {
  def put(key: String, value: A): F[Unit]
  def get(key: String): F[Option[A]]
}

object Storage {
  def apply[F[_], A](implicit ev: Storage[F, A]): Storage[F, A] = ev
}