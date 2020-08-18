package dorr.modules.impl

import cats.{Functor, MonadError}
import dorr.Configuration.Config
import dorr.http.AuthData
import dorr.modules.dsl.AuthProvider
import dorr.modules.dsl.AuthProvider.{Key, Token}
import dorr.util.HttpClient
import dorr.util.storage.Prefixed.prefixedStorage
import dorr.util.storage.Storage
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import io.circe.parser.decode
import tofu.BracketThrow
import tofu.generate.GenUUID
import tofu.syntax.monadic._

class VkAuthProvider[F[_] : HttpClient : Functor : BracketThrow : GenUUID : Storage[*[_], AuthData]](config: Config) extends AuthProvider[F] {
  case class Response(accessToken: String, expiresIn: Long, userId: Long)

  implicit val circeConf = Configuration.default.withSnakeCaseMemberNames
  implicit val audioDecoder: Decoder[Response] = deriveConfiguredDecoder

  val authDataStorage = prefixedStorage(AuthData.tablePrefix)

  def responseToAuthData(content: String, serviceToken: String): Either[io.circe.Error, AuthData] = {
    decode[Response](content) map { resp =>
      AuthData(
        expiresIn = resp.expiresIn,
        userId = resp.userId,
        serviceToken = serviceToken,
        providerToken = resp.accessToken,
        provider = "vk"
      )
    }
  }

  def requestAuthData(code: Token): F[AuthData] = {
    val params = Seq(
      ("client_id", config.app.id.toString),
      ("client_secret", config.app.secret),
      ("code", code),
      ("redirect_uri", config.oauth.clbAddr)
    )

    for {
      response  <- HttpClient[F].https("oauth.vk.com", "/access_token", params: _*)
      servToken <- GenUUID[F].randomUUID
      authData  <- MonadError[F, Throwable].fromEither(responseToAuthData(response, servToken.toString))
    } yield authData
  }


  //TODO profile creation
  override def auth(code: Token): F[Key] = {
    for {
      data  <- requestAuthData(code)
      _     <- authDataStorage.put(data.serviceToken, data)
    } yield data.serviceToken
  }
}
