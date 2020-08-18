package dorr.http

import java.net.URI

import cats.Monad
import dorr.Configuration.Config
import dorr.contrib.tschema.OAuthRedirect.RedirectParams
import dorr.modules.dsl.AuthProvider
import logstage.LogIO
import org.apache.http.client.utils.URIBuilder
import ru.tinkoff.tschema.finagle.{Rejection, Routed}
import tofu.generate.GenRandom
import tofu.syntax.monadic._

class AuthHandler[F[_]: Monad: Routed: AuthProvider: GenRandom: LogIO](config: Config) {

  //TODO
  //instead of Long seed we could use more complex structure for the state
  //so it is possible to encode some information inside (like `come_from`)
  //as well as it gives us more reliable security model
  def auth: F[RedirectParams] =
    GenRandom[F].nextLong map { seed =>
      new URIBuilder(config.oauth.authorizationUrl)
        .addParameter("client_id", config.app.id.toString)
        .addParameter("redirect_uri", config.oauth.clbAddr)
        .addParameter("response_type", "code")
        .addParameter("v", "5.103")
        .addParameter("state", seed.toString)
        .build() -> seed.toString
    }

  def callback(state: String, oauth: OAuth): F[URI] = {
    if (oauth.state == state) {
      AuthProvider[F].auth(oauth.code) map { token =>
        new URIBuilder()
          .setPath("/secure/success")
          .addParameter("token", token)
          .build()
      }
    } else {
      LogIO[F].warn("Rejected oAuth callback request: bad state") >>
        Routed[F].reject(Rejection.body("Unauthorized"))
    }
  }

  def success: F[String] = "Success".pure
}