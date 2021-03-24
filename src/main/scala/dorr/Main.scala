package dorr

import cats.Applicative
import cats.effect.LiftIO
import izumi.distage.model.effect.DIEffect
import izumi.distage.plugins.PluginConfig
import izumi.distage.roles
import izumi.distage.roles.RoleAppMain
import izumi.fundamentals.reflection.Tags.TagK
import monix.eval.Task

class RoleApp[F[_]: LiftIO: DIEffect: TagK] extends roles.RoleAppLauncher.LauncherF[F]() {
  override protected def pluginConfig: PluginConfig =
    PluginConfig.const(new MainPlugin)
}

object Main extends RoleAppMain.Default(launcher = new RoleApp[Task]) {

  class Program[F[_]: Applicative] {

    def run: F[Unit] =
      Applicative[F].unit
  }
}
