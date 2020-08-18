package dorr.http

import derevo.derive
import dorr.Configuration.Config
import dorr.contrib.tschema.Mime
import dorr.contrib.tschema.RoutesDslCustom._
import ru.tinkoff.tschema.param.HttpParam
import ru.tinkoff.tschema.swagger.{OAuthConfig, OpenApiFlow, Swagger}
import ru.tinkoff.tschema.syntax._

@derive(HttpParam, Swagger)
final case class OAuth(code: String, state: String)
final case class Cookie(state: String)
case class From(bearer: String)
case class To(userId: Long)

class Routes(val oaConf: OAuthConfig) {
  val upload = operation("upload") |> oauth[From, To]("authData", oaConf) |> post |> reqBody[Mime.AudioMpeg] |> $$[String]
  val status = operation("status") |> oauth[From, To]("authData", oaConf) |> get |> $$[String]

  val auth   = prefix("secure") |> ((
    operation("callback") |> get |> cookie[String]("state") |> queryParam[OAuth]("oauth") |> redirect
  ) <|> (
    operation("success") |> get |> $$[String]
  ))
}