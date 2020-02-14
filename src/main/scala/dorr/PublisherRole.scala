package dorr

import cats.effect.Bracket
import dorr.Main.Program
import izumi.distage.model.definition.DIResource
import izumi.distage.model.effect.DIEffect
import izumi.distage.roles.model.{RoleDescriptor, RoleService}
import izumi.fundamentals.platform.cli.model.raw.RawEntrypointParams

class PublisherRole[F[_]: Program: Bracket[*[_], Throwable]: DIEffect] extends RoleService[F] {
  override def start(roleParameters: RawEntrypointParams, freeArgs: Vector[String]): DIResource.DIResourceBase[F, Unit] = {
    DIResource.liftF(
      implicitly[Program[F]].run
    )
  }
}

object PublisherRole extends RoleDescriptor {
  override def id: String = "publisher"
}
