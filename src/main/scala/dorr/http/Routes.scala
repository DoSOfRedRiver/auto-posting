package dorr.http

import derevo.derive
import dorr.contrib.tschema.Mime
import dorr.contrib.tschema.RoutesDslCustom._
import ru.tinkoff.tschema.param.HttpParam
import ru.tinkoff.tschema.swagger.{OpenApiFlow, OpenApiFlows, Swagger}
import ru.tinkoff.tschema.swagger.SwaggerMapper.OAuthConfig
import ru.tinkoff.tschema.syntax._

object Routes {
  @derive(HttpParam, Swagger)
  final case class OAuth(code: String, state: String)
  final case class Cookie(state: String)

  val upload = operation("upload") |> oAuth[AuthData]("authData") |> post |> reqBody[Mime.AudioMpeg] |> $$[String]
  val status = operation("status") |> oAuth[AuthData]("authData") |> get |> $$[String]

  val auth   = prefix("secure") |> ((
    operation("auth")   |> get |> oAuthRedirect
  ) <|> (
    operation("callback") |> get |> cookie[String]("state") |> queryParam[OAuth]("oauth") |> redirect
  ) <|> (
    operation("success") |> get |> $$[String]
  ))

  case class From(id: Long, meta: String)
  case class To(userId: Long, username: String)

  val authFlow = OpenApiFlow.AuthorizationCode(
    authorizationUrl = "https://oauth.vk.com/authorize",
    tokenUrl = "http://127.0.0.1:9191/secure/auth"
  )

  val flows: OpenApiFlows = OpenApiFlows(authorizationCode = Some(authFlow))
  val config: OAuthConfig = OAuthConfig("oauthUser", flows)

  val test = operation("test") |> get |> oauth[From, To]("paramName", config) |> $$[String]
  val test1 = operation("test1") |> get |> bearerAuth[To]("pook", "kek") |> $$[String]
}
