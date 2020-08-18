package dorr.contrib.tschema

import ru.tinkoff.tschema.typeDSL.Complete

object RoutesDslCustom {
  val redirect: Complete[Redirect] = new Complete
  val oAuthRedirect: Complete[OAuthRedirect] = new Complete
  val setCookie: Complete[SetCookie] = new Complete
}
