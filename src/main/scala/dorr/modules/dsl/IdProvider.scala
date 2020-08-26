package dorr.modules.dsl

import IdProvider._
import dorr.modules.defs.IdData

trait IdProvider[F[_]] {
  def auth(code: Code): F[IdData]
}

object IdProvider {
  type Code = String

  def apply[F[_]](implicit ev: IdProvider[F]): IdProvider[F] = ev
}
