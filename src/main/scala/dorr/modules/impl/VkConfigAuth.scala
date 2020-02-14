package dorr.modules.impl

import cats.Applicative
import cats.syntax.applicative._
import distage.plugins.PluginBase
import dorr.Config
import dorr.modules.dsl.Auth
import dorr.modules.dsl.Auth.Key

class VkConfigAuth[F[_]: Applicative](conf: Config) extends Auth[F] {
  override def auth: F[Key] = conf.accessToken.pure
}
