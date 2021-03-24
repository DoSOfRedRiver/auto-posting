package dorr.http

import cats.Monad
import dorr.modules.defs.Profile
import dorr.util.storage.Storage
import tofu.syntax.monadic._

trait ProfileHandler[F[_]] {
  implicit val profileStorage: Storage[F, Profile]
  implicit val functor: Monad[F]

  def profile(id: String): F[Profile] =
    profileStorage.get(id).map(_.get)
}
