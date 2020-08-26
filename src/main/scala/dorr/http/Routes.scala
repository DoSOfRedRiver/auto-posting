package dorr.http

import derevo.derive
import dorr.contrib.tschema.Mime
import dorr.contrib.tschema.RoutesDslCustom._
import dorr.modules.defs.SessionData
import ru.tinkoff.tschema.param.HttpParam
import ru.tinkoff.tschema.swagger.{OAuthConfig, Swagger}
import ru.tinkoff.tschema.syntax._

@derive(HttpParam, Swagger)
final case class OAuth(code: String, state: String)
final case class StateCookie(state: String)
case class To(userId: String)

class Routes(val oaConf: OAuthConfig) {
  val upload = operation("upload") |> oauth[SessionData, To]("authData", oaConf) |> post |> reqBody[Mime.AudioMpeg] |> $$[String]
  val status = operation("status") |> oauth[SessionData, To]("authData", oaConf) |> get |> $$[String]

  val auth   = prefix("secure") |> ((
    operation("auth")     |> get |> oAuthRedirect
  ) <|> (
    operation("callback") |> get |> cookie[String]("state") |> queryParam[OAuth]("oauth") |> authenticate
  ) <|> (
    operation("success")  |> get |> $$[String]
  ))
}