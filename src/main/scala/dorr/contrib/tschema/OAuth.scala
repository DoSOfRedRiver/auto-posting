package dorr.contrib.tschema

import cats.arrow.FunctionK
import cats.data.OptionT
import cats.{Applicative, FlatMap, Monad}
import com.twitter.finagle.http.{Request, Response}
import dorr.http.AuthData
import ru.tinkoff.tschema.common.Name
import ru.tinkoff.tschema.finagle.Serve.Add
import ru.tinkoff.tschema.finagle.{Rejection, Routed, Serve}
import ru.tinkoff.tschema.swagger.MkSwagger.{PathSeq, PathSpec, TagInfo, TypePool}
import ru.tinkoff.tschema.swagger.{MkSwagger, OpenApiFlow, OpenApiOp, OpenApiParam, OpenApiSecurity, OpenApiSecurityScheme, OpenApiSecurityType, SwaggerBuilder, SwaggerContent, SwaggerMapper}
import ru.tinkoff.tschema.typeDSL.DSLAtomAuth
import shapeless.labelled.FieldType
import shapeless.{::, HList, HNil}
import tofu.syntax.monadic._

import scala.collection.immutable.TreeMap

class MkOAuth[x] {
  def apply[name <: String with Singleton](n: name) =
    new OAuthAtom[x, name]
}

trait OAuth[H[_], A, E] {
  def apply(e: E): H[Option[A]]
}

final class OAuthAtom[x, name] extends DSLAtomAuth

object OAuthAtom {
  implicit def oAuthServe[H[_]: Routed: Monad, In <: HList, name, V, E](implicit
    auth: OAuth[H, V, E], fromReq: FromReq[H, E]
  ): Add[OAuthAtom[V, name], H, In, name, V] =
      Serve.add {
        OptionT(fromReq.get)
          .flatMap(e => OptionT(auth(e)))
          .getOrElseF(Routed[H].reject(Rejection.unauthorized))
      }

  //TODO extract description
  val security = OpenApiSecurity(
    `type` = OpenApiSecurityType.oauth2,
    description = Some("This is a description for OAuth2"),
    flows = None) //Some(List(OpenApiFlow.AuthorizationCode("https://oauth.vk.com", "https://oauth.vk.com/token", None, Map.empty))))

  implicit def swaggerMapper[x, name]: SwaggerMapper[OAuthAtom[x, name]] = {
    SwaggerMapper.fromAuth("kek", security)
  }
}
