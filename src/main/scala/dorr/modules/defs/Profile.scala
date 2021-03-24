package dorr.modules.defs

import derevo.derive
import ru.tinkoff.tschema.swagger.Swagger
import ru.tinkoff.tschema.finagle.circeInstances._
import derevo.circe.encoder


@derive(encoder, Swagger)
case class Profile(name: String, internalId: String)

object Profile {
  val tablePrefix = "profile"
}