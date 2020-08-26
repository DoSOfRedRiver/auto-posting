package dorr.http

import cats.Monad
import dorr.Configuration.Config
import dorr.contrib.tschema.OAuthRedirect.RedirectParams
import dorr.modules.defs.SessionData
import dorr.modules.dsl.{AuthManager, IdProvider}
import logstage.LogIO
import org.apache.http.client.utils.URIBuilder
import ru.tinkoff.tschema.finagle.{LiftHttp, Rejection, Routed}
import tofu.generate.GenRandom
import tofu.syntax.monadic._

class AuthHandler[H[_]: Routed: LogIO: Monad, F[_]: AuthManager: GenRandom: IdProvider: Monad](config: Config, Lift: LiftHttp[H, F]) {

  //TODO
  //instead of Long seed we could use more complex structure for the state
  //so it is possible to encode some information inside (like `come_from`)
  //as well as it gives us more reliable security model
  def auth: H[RedirectParams] =
    Lift(GenRandom[F].nextLong map { seed =>
      new URIBuilder(config.oauth.authorizationUrl)
        .addParameter("client_id", config.app.id.toString)
        .addParameter("redirect_uri", config.oauth.clbAddr)
        .addParameter("response_type", "code")
        .addParameter("v", "5.103")
        .addParameter("state", seed.toString)
        .build() -> seed.toString
    })

  def callback(state: String, oauth: OAuth): H[SessionData] = {
    if (oauth.state == state) {
      Lift(IdProvider[F].auth(oauth.code) >>= AuthManager[F].authenticate)
    } else {
      LogIO[H].warn("Rejected oAuth callback request: bad state") >>
        Routed[H].reject(Rejection.unauthorized)
    }
  }

  def success: H[String] = "Success".pure[H]
}