package dorr.modules.impl

import cats.effect.concurrent.Deferred
import cats.effect.{Bracket, Timer}
import cats.instances.either._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.{Defer, Monad}
import dorr.Configuration.Config
import dorr.modules.dsl.Auth
import logstage.LogIO
import org.http4s._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.blaze._
import tofu.Start
import tofu.concurrent.{MakeDeferred, TryableDeferreds}
import tofu.syntax.start._

class VkOAuth[F[_]: Monad: Timer: LogIO: Defer: TryableDeferreds: Start: EntityDecoder[*[_], String]: Bracket[*[_], Throwable]](
  conf: Config,
  clientBuilder: BlazeClientBuilder[F],
  serverBuilder: BlazeServerBuilder[F],
) extends Auth[F] {
  val dsl = new Http4sDsl[F]{};
  import dsl._

  object CodeQueryParamMatcher extends QueryParamDecoderMatcher[String]("code")

  val permissions = new {
    val photos  = 4
    val wall    = 8192
    val offline = 65536
  }

  def auth: F[String] = {
    val redirect = s"${conf.oauth.redirectAddr}/auth"

    val authorize =  uri"https://oauth.vk.com/authorize" +?
      ("client_id", conf.app.id) +?
      ("redirect_uri", redirect) +?
      ("scope", permissions.wall + permissions.offline) +?
      ("response_type", "token") +?
      ("v", "5.103")

    val accessTokenUri = uri"https://oauth.vk.com/access_token" +?
      ("client_secret", conf.app.secret) +?
      ("redirect_uri", redirect) +?
      ("client_id", conf.app.id)

    def authApp(codeDef: Deferred[F, String]) = HttpRoutes.of[F] {
      case GET -> Root / "auth" :? CodeQueryParamMatcher(code) =>
        Ok() <* LogIO[F].info(s"Got code: $code") <* codeDef.complete(code)
    }.orNotFound

    val authCode = MakeDeferred.tryable[F, String]

    def client(token: String) = clientBuilder.resource.use { client =>
      val respUri = accessTokenUri +? ("code", token)
      client.expect[String](respUri) <* LogIO[F].info(s"Response uri: $respUri")
    }

    def startServer(codeDef: Deferred[F, String]) = serverBuilder
      .withHttpApp(authApp(codeDef))
      .resource.use(_ => codeDef.get).start

    for {
      ac    <- authCode
      _     <- startServer(ac)
      _     <- LogIO[F].info(s"Url: $authorize")
      code  <- ac.get
      _     <- LogIO[F].info(s"Run client")
      res   <- client(code)
    } yield res
  }
}
