package dorr.initializers

import cats.{Functor, Monad}
import dorr.contrib.tschema.Authenticate
import dorr.modules.defs.Session
import dorr.util.storage.Prefixed.prefixedStorage
import dorr.util.storage.Storage
import ru.tinkoff.tschema.finagle.{LiftHttp, Routed}
import tofu.Context
import tofu.syntax.monadic._

class HasSession[H[_]: Routed: Monad, F[_]: Storage[*[_], String]](lift: LiftHttp[H, F]) extends Context[H] {
  private val sessionStorage = prefixedStorage("session")

  override type Ctx = Option[Session]

  override def functor: Functor[H] = Monad[H]

  //TODO replace with auth mechanism?
  override def context: H[Ctx] = for {
    req       <- Routed[H].request
    sessionId =  req.cookies(Authenticate.sessionId).value
    userId    <- lift(sessionStorage.get(sessionId))
  } yield userId map Session
}
