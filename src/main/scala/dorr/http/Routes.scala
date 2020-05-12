package dorr.http

import dorr.contib.Mime
import ru.tinkoff.tschema.syntax.{$$, operation, post, reqBody}
import ru.tinkoff.tschema.syntax._

object Routes {
  val upload = post |> operation("upload") |> reqBody[Mime.AudioMpeg] |> $$[String]
  val status = get |> operation("status") |> $$[String]
}
