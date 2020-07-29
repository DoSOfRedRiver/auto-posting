package dorr.contrib.tschema

trait FromReq[F[_], A] {
  def get: F[Option[A]]
}

object FromReq {
  def apply[F[_], A](implicit ev: FromReq[F, A]): FromReq[F, A] = ev
}
