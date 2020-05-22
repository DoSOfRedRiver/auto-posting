package dorr.util.storage

sealed trait Prefixed[F[_], A, Prefix <: String with Singleton] extends Storage[F, A]

object Prefixed {

  def prefixedStorage[F[_], A](prefix: String)(implicit storage: Storage[F, A]) =
    new Prefixed[F, A, prefix.type] {
      private def withPrefix(key: String): String = s"${prefix}-$key"

      override def put(key: String, value: A): F[Unit] =
        storage.put(withPrefix(key), value)


      override def get(key: String): F[Option[A]] =
        storage.get(withPrefix(key))
    }
}
