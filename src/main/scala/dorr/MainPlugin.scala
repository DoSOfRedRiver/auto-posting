package dorr

import java.time.format.DateTimeFormatter

import cats.effect.{Async, ConcurrentEffect, ContextShift, Resource, Sync, Timer}
import cats.instances.list._
import cats.syntax.foldable._
import cats.{Applicative, Functor, Monad, MonadError, ~>}
import com.twitter.finagle.http.Response
import com.twitter.util.Future
import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.httpclient.HttpTransportClient
import distage.{DIKey, ModuleDef, TagK}
import dorr.Configuration.Config
import dorr.Main.Program
import dorr.contrib.tofu.Execute
import dorr.http._
import dorr.initializers.{BackgroundProcess, HasSession, HttpServerInit}
import dorr.modules.AuthMeta.{FromProv, VkOAuth}
import dorr.modules.defs.{IdData, Profile, SessionData}
import dorr.modules.dsl._
import dorr.modules.impl._
import dorr.modules.impl.events.{VkApi, VkApiImpl, VkEvents}
import dorr.modules.{DbAuthorization, OAuthProvision}
import dorr.util.instances._
import dorr.util.storage.RocksStorage.RocksGen
import dorr.util.storage.{RocksStorage, Storage}
import dorr.util.{Bytes, File, FinagleHttpClient, HttpClient, Time, modules}
import izumi.distage.config.ConfigModuleDef
import izumi.distage.config.codec.PureconfigAutoDerive
import izumi.distage.constructors.ClassConstructor
import izumi.distage.model.definition.Axis
import izumi.distage.model.effect.DIEffectRunner
import izumi.distage.plugins.PluginDef
import izumi.logstage.api.IzLogger
import logstage.LogIO
import monix.eval.Task
import org.rocksdb.{Options, RocksDB}
import pureconfig.configurable._
import ru.tinkoff.tschema.finagle._
import ru.tinkoff.tschema.finagle.envRouting.TaskRouting.TaskHttp
import ru.tinkoff.tschema.swagger.{OAuthConfig, OpenApiFlow}
import ru.tinkoff.tschema.utils.Provision
import tofu.{BracketThrow}
import tofu.generate.{GenRandom, GenUUID}
import tofu.lift.{Lift, Unlift}
import tofu.syntax.monadic._

//TODO
object Authentication extends Axis {
  case object ConfigAuth extends AxisValueDef
  case object OAuth extends AxisValueDef
}

class MainPlugin extends PluginDef {
  implicit val sc = monix.execution.Scheduler.global

  type SecuredHandler[F[_], H[_]] = UploadHandler[F, H] with ProfileHandler[F] with StatusHandler[F]

  val logger = IzLogger()

  implicit val log = LogIO.fromLogger[Task](logger)
  implicit val httpLog = LogIO.fromLogger[TaskHttp](logger)


  implicit val localDateConvert = localTimeConfigConvert(DateTimeFormatter.ISO_TIME)
  implicit val configReader: pureconfig.ConfigReader[Config] = PureconfigAutoDerive[Config]

  implicit val diEffectRunner = new DIEffectRunner[Task] {
    override def run[A](f: => Task[A]) = f.runSyncUnsafe()
  }

  implicit def liftCompose[F[_], G[_], H[_]](implicit
    fg: Lift[F, G], gh: Lift[G, H]
  ) = new Lift[F, H] {
    override def lift[A](fa: F[A]): H[A] = gh.lift(fg.lift(fa))
  }


  val backgroundProcessesKey = DIKey.get[Set[Unit]].named("background-tasks")

  def infrastructure[F[_]: TagK: LogIO: ConcurrentEffect: Timer] =
    modules.Misc ++ modules.DIEffects ++ ConfigModule ++ PublisherRole ++ Rocks ++ HttpServer[TaskHttp, Task]

  def implementations[F[_]: TagK: ConcurrentEffect: LogIO: Timer] =
    Program ++ Modules[Task, TaskHttp] ++ Client ++ Storages ++ Storages[TaskHttp] ++ Instruments[Task, TaskHttp] ++ HttpHandlers[Task, TaskHttp] ++ HttpRoutes[Task, TaskHttp]

  include(implementations[Task] ++ infrastructure[Task])


  def HttpHandlers[F[_]: TagK, H[_]: TagK] = new ModuleDef {
    make[SecuredHandler[F, H]]
    make[AuthHandler[H, F]]
  }

  def HttpRoutes[F[_]: Applicative: Sync: Unlift[*[_], H]: TagK, H[_]: TagK: MonadError[*[_], Throwable]: LiftHttp[*[_], F]: RoutedPlus] = new ModuleDef {
    import ru.tinkoff.tschema.finagle.circeInstances._

    implicit val liftId = new LiftHttp[H, H] {
      override def apply[A](fa: H[A]): H[A] = fa
    }

    make[SwaggerGen[H]]
    make[Routes]
    make[OAuthConfig].from { cfg: Config =>
      OAuthConfig("vkOAuth").flow {
        OpenApiFlow.authorizationCode(
          authorizationUrl = cfg.oauth.authorizationUrl,
          tokenUrl = cfg.oauth.tokenUrl
        )
      }
    }
    make[Provision[H, SessionData]].from[OAuthProvision[H]]

    //DOES NOT COMPILE WITHOUT MONAD INSTANCE FOR H
    //TODO bring routes into the scope?
    many[H[Response]].add { (auth: VkOAuth[H], sh: SecuredHandler[F, H], routes: Routes, prov: FromProv[H]) =>
      implicit val dumb = auth
      implicit val dumb1  = prov

      MkService[H](routes.secured)(sh)
    }

    many[H[Response]].add { (authHandler: AuthHandler[H, F], routes: Routes) =>
      MkService[H](routes.auth)(authHandler)
    }

    many[H[Response]].add { (_: SwaggerGen[H]).route }

    make[HasSession[H, F]].from[HasSession[H, F]]
  }


  def HttpServer[H[_]: TagK: RoutedPlus: LogIO: Async: Lift[Task, *[_]], F[_]: TagK: Async: ContextShift](
    implicit R: RunHttp[H, F], L: Lift[F, H], Lift: LiftHttp[H, F]
  ) = new ModuleDef {
    addImplicit[Lift[F, H]]
    addImplicit[LiftHttp[H, F]]
    addImplicit[RunHttp[H, F]]
    addImplicit[RoutedPlus[H]]
    addImplicit[Routed[H]]
    addImplicit[BracketThrow[H]]
    addImplicit[Sync[H]]
    addImplicit[Monad[H]]
    addImplicit[Functor[H]]
    addImplicit[LogIO[H]]
    //TODO
    make[Execute[Future, F]].from { Execute.asyncExecuteTwitter[F] }

    make[VkOAuth[H]].from[DbAuthorization[H, F]]

    many[BackgroundProcess[F]].add[HttpServerInit[H, F]]
  }

  def Instruments[F[_]: TagK: Time: File: Sync, H[_]: TagK: GenUUID: Sync] = new ModuleDef {
    addImplicit[Time[F]]
    addImplicit[File[F]]
    addImplicit[GenUUID[F]]
    make[GenRandom[F]].fromEffect {
      GenRandom.instance[F, F](secure = true)
    }
    make[HttpClient[F]].from[FinagleHttpClient[F]]
  }

  def Modules[F[_] : TagK : Sync, H[_]: TagK] = new ModuleDef {
    make[Publish[F]].from[VkPublish[F]]
    make[Events[F]].from[VkEvents[F]]
    make[VkApi[F]].from[VkApiImpl[F]]
    make[Schedule[F]].from[VkSchedule[F]]
    make[IdProvider[F]].from[VkIdProvider[F]]
    make[AuthManager[F]].from[GohAuthManager[F]]
  }

  def Storages[F[_]: TagK: Sync] = new ModuleDef {
    make[Storage[F, Array[Byte]]].from[RocksStorage[F]]

    make[Storage[F, Profile]].from(RocksGen(Bytes.asSerializable[Profile]).create[F])
    make[Storage[F, IdData]].from(RocksGen(Bytes.asSerializable[IdData]).create[F])
    make[Storage[F, String]].from(RocksGen(Bytes.stringBytes).create[F])
    make[Storage[F, Int]].from(RocksGen(Bytes.intBytes).create[F])
  }

  def Program[F[_]: TagK: Applicative] = new ModuleDef {
    many[Unit].named(backgroundProcessesKey.id).addEffect { ps: Set[BackgroundProcess[F]] =>
      ps.toList.traverse_(_.start)
    }

    make[Program[F]].from {
      ClassConstructor[Program[F]]
        .addDependency(backgroundProcessesKey)
    }
  }

  def Client[F[_] : TagK : Sync] = new ModuleDef {
    make[VkApiClient].fromEffect(Sync[F].delay {
      val transportClient = new HttpTransportClient
      new VkApiClient(transportClient)
    })
  }

  def Rocks[F[_]: TagK: Sync] = new ModuleDef {
    RocksDB.loadLibrary()

    make[Options].fromResource(Resource.fromAutoCloseable {
      Sync[F].delay(new Options().setCreateIfMissing(true))
    })

    make[RocksDB].fromResource((opts: Options, conf: Config) =>
      Resource.fromAutoCloseable(
        Sync[F].delay(RocksDB.open(opts, conf.database.path))
      )
    )
  }

  def ConfigModule[F[_]: Sync: TagK] = new ConfigModuleDef {
    makeConfig[Config]("conf")
  }

  def PublisherRole[F[_]: TagK] = new ModuleDef {
    make[PublisherRole[F]]
  }
}
