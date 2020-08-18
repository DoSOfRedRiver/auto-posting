package dorr.modules.dsl

import AuthProvider._

trait AuthProvider[F[_]] {
  def auth(token: Token): F[Key]
}

object AuthProvider {
  type Key = String
  type Token = String

  def apply[F[_]](implicit ev: AuthProvider[F]): AuthProvider[F] = ev
}
