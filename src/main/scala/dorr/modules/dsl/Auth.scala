package dorr.modules.dsl

trait Auth[F[_]] {
  def auth: F[Auth.Key]
}

object Auth {
  type Key = String

  def apply[F[_]](implicit ev: Auth[F]): Auth[F] = ev
}
