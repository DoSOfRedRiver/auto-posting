package dorr.http

import derevo.derive
import dorr.contrib.tschema.Mime
import dorr.contrib.tschema.RoutesDslCustom._
import dorr.modules.defs.{Profile, SessionData}
import ru.tinkoff.tschema.param.HttpParam
import ru.tinkoff.tschema.swagger.{OAuthConfig, Swagger}
import ru.tinkoff.tschema.syntax._

@derive(HttpParam, Swagger)
final case class OAuth(code: String, state: String)
final case class StateCookie(state: String)
case class To(userId: String)

class Routes(val oaConf: OAuthConfig) {
  val secured = oauth[SessionData, To]("authData", oaConf) |> ((
    operation("upload") |> post |> reqBody[Mime.AudioMpeg] |> $$[String]
  ) <|> (
    operation("status") |> get |> $$[String]
  ) <|> (
    operation("profile") |> get |> queryParam[String]("id") |> $$[Profile]
  ))

  val auth   = prefix("secure") |> ((
    operation("auth")     |> get |> oAuthRedirect
  ) <|> (
    operation("callback") |> get |> cookie[String]("state") |> queryParam[OAuth]("oauth") |> authenticate
  ))
}