package dorr.http

import derevo.derive
import dorr.contrib.{Mime, oAuthRedirect, redirect, setCookie}
import ru.tinkoff.tschema.param.HttpParam
import ru.tinkoff.tschema.syntax._

object Routes {
  @derive(HttpParam)
  final case class OAuth(code: String, state: String)
  final case class Cookie(state: String)

  val upload = operation("upload") |> bearerAuth[AuthData]("clients", "user") |> post |> reqBody[Mime.AudioMpeg] |> $$[String]
  val status = operation("status") |> bearerAuth[AuthData]("clients", "user") |> get |> $$[String]

  val auth   = prefix("secure") |> ((
    operation("auth")   |> get |> $$[oAuthRedirect]
  ) <|> (
    operation("callback") |> get |> cookie[String]("state") |> queryParam[OAuth]("oauth") |> $$[redirect]
  ) <|> (
    operation("success") |> get |> $$[String]
  ))
}
