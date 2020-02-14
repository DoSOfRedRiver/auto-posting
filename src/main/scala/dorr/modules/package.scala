package dorr

import dorr.modules.dsl.{Attachment, Audio, Event, Obj}
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

package object modules {
  implicit val circeConf = Configuration.default.withSnakeCaseMemberNames
  implicit val audioDecoder:  Decoder[Audio]      = deriveConfiguredDecoder
  implicit val attDecoder:    Decoder[Attachment] = deriveConfiguredDecoder
  implicit val objDecoder:    Decoder[Obj]        = deriveConfiguredDecoder
  implicit val eventDecoder:  Decoder[Event]      = deriveConfiguredDecoder
}
