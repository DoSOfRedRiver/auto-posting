package dorr.http


import cats.Monad
import dorr.http.AuthMeta.VkOAuth
import dorr.util.storage.Prefixed.prefixedStorage
import dorr.util.storage.Storage
import dorr.util.syntax.optionF._
import ru.tinkoff.tschema.finagle.Authorization.OAuth2
import ru.tinkoff.tschema.finagle.{Authorization, LiftHttp, Rejection, Routed}
import ru.tinkoff.tschema.utils.Provision
import tofu.syntax.monadic._

object AuthMeta {
  type VkOAuth[H[_]] = Authorization[OAuth2, H, To, From]
  type FromProv[H[_]] = Provision[H, From]
}

class DbAuthorization[H[_]: Monad: Routed, F[_]: Storage[*[_], AuthData]](implicit Lift: LiftHttp[H, F]) extends VkOAuth[H] {
  val authDataStorage = prefixedStorage(AuthData.tablePrefix)
  def reject[A] = Routed[H].reject[A](Rejection.unauthorized)

  override def apply(s: Option[From]): H[To] = s match {
    case Some(From(bearer)) =>
      val maybeData = Lift(authDataStorage.get(bearer))
      maybeData getOrElseF reject map (data => To(data.userId))
    case None =>
      reject
  }
}