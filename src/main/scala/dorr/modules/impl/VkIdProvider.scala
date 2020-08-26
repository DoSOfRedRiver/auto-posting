package dorr.modules.impl

import cats.syntax.monadError._
import cats.{Functor, MonadError}
import dorr.Configuration.Config
import dorr.modules.defs.IdData
import dorr.modules.dsl.IdProvider
import dorr.modules.dsl.IdProvider.Code
import dorr.util.HttpClient
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import io.circe.parser.decode
import tofu.BracketThrow
import tofu.syntax.monadic._

class VkIdProvider[F[_] : HttpClient : Functor : BracketThrow](config: Config) extends IdProvider[F] {

  case class Response(accessToken: String, expiresIn: Long, userId: Long)

  implicit val circeConf = Configuration.default.withSnakeCaseMemberNames
  implicit val audioDecoder: Decoder[IdData] = deriveConfiguredDecoder

  override def auth(code: Code): F[IdData] = {
    val params = Seq(
      ("client_id", config.app.id.toString),
      ("redirect_uri", config.oauth.clbAddr),
      ("client_secret", config.app.secret),
      ("code", code)
    )

    for {
      response <- HttpClient[F].https("oauth.vk.com", "/access_token", params: _*)
      authData <- MonadError[F, Throwable]
        .fromEither(decode[IdData](response))
        .adaptError(new IllegalStateException(
          "Could not parse IdP response, possibly bad configuration provided",
          _
        ))
    } yield authData
  }
}