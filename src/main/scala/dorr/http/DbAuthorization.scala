package dorr.http


import cats.{Applicative, FlatMap, Monad}
import dorr.contrib.tschema.{FromReq, OAuth}
import dorr.util.storage.Prefixed.prefixedStorage
import dorr.util.storage.Storage
import dorr.util.syntax.optionF._
import ru.tinkoff.tschema.finagle.{BearerToken, LiftHttp, Rejection, Routed}
import tofu.syntax.monadic._
import cats.instances.option._

case class AuthMeta(bearer: String)

object AuthMeta {
  type OAuth1[F[_], A] = OAuth[F, A, AuthMeta]

  implicit def authMetaFromReq[H[_]: Routed: FlatMap]: FromReq[H, AuthMeta] = new FromReq[H, AuthMeta] {
    override def get: H[Option[AuthMeta]] = Routed[H].request.map { req =>
      (req.authorization >>= BearerToken.unapply) map AuthMeta.apply
    }
  }
}

class DbAuthorization[H[_]: Monad: Routed, F[_]: Storage[*[_], AuthData]](implicit Lift: LiftHttp[H, F]) extends OAuth[H, AuthData, AuthMeta] {
  val authDataStorage = prefixedStorage(AuthData.tablePrefix)

  //TODO remove reject?
  override def apply(e: AuthMeta): H[Option[AuthData]] =
    Lift(authDataStorage.get(e.bearer))
}