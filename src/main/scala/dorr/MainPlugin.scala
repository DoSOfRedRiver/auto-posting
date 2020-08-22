package dorr

import java.time.format.DateTimeFormatter

import cats.data.OptionT
import cats.effect.{Async, ConcurrentEffect, ContextShift, Resource, Sync, Timer}
import cats.instances.list._
import cats.syntax.foldable._
import cats.{Applicative, Functor, Monad, MonadError}
import com.twitter.finagle.http.Response
import com.twitter.util.Future
import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.httpclient.HttpTransportClient
import distage.{DIKey, ModuleDef, TagK}
import dorr.Configuration.Config
import dorr.Main.Program
import dorr.contrib.tofu.Execute
import dorr.http.AuthMeta.{FromProv, VkOAuth}
import dorr.http.{From, To, _}
import dorr.initializers.{BackgroundProcess, HttpServerInit}
import dorr.modules.dsl.{AuthProvider, Events, Publish, Schedule}
import dorr.modules.impl._
import dorr.modules.impl.events.{VkApi, VkApiImpl, VkEvents}
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
import ru.tinkoff.tschema.finagle.Authorization.OAuth2
import ru.tinkoff.tschema.finagle._
import ru.tinkoff.tschema.finagle.envRouting.TaskRouting.TaskHttp
import ru.tinkoff.tschema.swagger.{OAuthConfig, OpenApiFlow}
import ru.tinkoff.tschema.utils.Provision
import tofu.BracketThrow
import tofu.generate.{GenRandom, GenUUID}
import tofu.lift.Lift
import tofu.syntax.monadic._

//TODO
object Authentication extends Axis {
  case object ConfigAuth extends AxisValueDef
  case object OAuth extends AxisValueDef
}

class MainPlugin extends PluginDef {
  implicit val sc = monix.execution.Scheduler.global

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
    Program ++ Modules[Task, TaskHttp] ++ MainFunctionalModule ++ Client ++ Storage ++ Storage[TaskHttp] ++ Instruments[Task, TaskHttp] ++ HttpHandlers[Task, TaskHttp] ++ HttpRoutes[Task, TaskHttp]

  include(implementations[Task] ++ infrastructure[Task])


  def HttpHandlers[F[_]: TagK, H[_]: TagK] =
    AddHandlers[F] ++ AddHandlers[H]


  def AddHandlers[F[_]: TagK] = new ModuleDef {
    make[UploadHandler[F]]
    make[AuthHandler[F]]
  }

  def HttpRoutes[F[_]: Applicative: Sync: TagK, H[_]: TagK: MonadError[*[_], Throwable]: LiftHttp[*[_], F]: RoutedPlus: GenUUID] = new ModuleDef {
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
    make[Provision[H, From]].from { routed: Routed[H] =>
      (() => Routed[H].request.map { req =>
        //TODO
        Some(From("bearer"))
      }): Provision[H, From]
    }

    //DOES NOT COMPILE WITHOUT MONAD INSTANCE FOR H
    //TODO bring routes into the scope?
    many[H[Response]].add { (auth: VkOAuth[H], uh: UploadHandler[F], routes: Routes, prov: FromProv[H]) =>
      implicit val dumb = auth
      implicit val dumb1  = prov
      MkService[H](routes.upload)(uh)
    }

    many[H[Response]].add { (authHandler: AuthHandler[H], routes: Routes) =>
      MkService[H](routes.auth)(authHandler)
    }

    many[H[Response]].add { (auth: VkOAuth[H], routes: Routes, prov: FromProv[H]) =>
      implicit val dumb = auth
      implicit val dumb1  = prov
      MkService[H](routes.status)(new {
        def status: F[String] = "Alive".pure[F]
      })
    }

    many[H[Response]].add { (_: SwaggerGen[H]).route }
  }


  def HttpServer[H[_]: TagK: RoutedPlus: LogIO: Async: Lift[Task, *[_]]: ContextShift, F[_]: TagK](
    implicit R: RunHttp[H, F], L: Lift[F, H], LH: LiftHttp[H, F]
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
    make[Execute[Future, H]].from { Execute.asyncExecuteTwitter[H] }

    make[VkOAuth[H]].from[DbAuthorization[H, F]]

    many[BackgroundProcess[F]].add[HttpServerInit[H, F]]
  }

  def Instruments[F[_]: TagK: Time: File: Sync, H[_]: TagK: GenUUID: Sync] = new ModuleDef {
    addImplicit[Time[F]]
    addImplicit[File[F]]
    addImplicit[GenUUID[H]]
    make[GenRandom[H]].fromEffect {
      GenRandom.instance[F, H](secure = true)
    }
    make[HttpClient[H]].from[FinagleHttpClient[H]]
  }

  def Modules[F[_] : TagK : Sync, H[_]: TagK] = new ModuleDef {
    make[Publish[F]].from[VkPublish[F]]
    make[Events[F]].from[VkEvents[F]]
    make[VkApi[F]].from[VkApiImpl[F]]
    make[Schedule[F]].from[VkSchedule[F]]
    make[AuthProvider[H]].from[VkAuthProvider[H]]
  }

  def Storage[F[_]: TagK: Sync] = new ModuleDef {
    make[Storage[F, Array[Byte]]].from[RocksStorage[F]]

    make[Storage[F, String]].from {
        implicit store: Storage[F, Array[Byte]] =>
          new RocksGen[F, String]()
    }

    make[Storage[F, Int]].from {
      implicit store: Storage[F, String] =>
        new Storage[F, Int] {
          override def put(key: String, value: Int) =
            store.put(key, value.toString)

          override def get(key: String) =
            OptionT(store.get(key)).map(_.toInt).value
        }
    }

    make[Storage[F, AuthData]].from { implicit storage: Storage[F, Array[Byte]] =>
      new Storage[F, AuthData] {
        val serialize = Bytes.asSerializable[AuthData]

        override def put(key: String, value: AuthData): F[Unit] =
          Sync[F].delay(serialize.to(value)) >>= (storage.put(key, _))

        override def get(key: String): F[Option[AuthData]] =
          storage.get(key) >>= { mbBytes =>
            Sync[F].delay(mbBytes.map(serialize.from))
          }
      }
    }
  }

  def MainFunctionalModule[F[_] : TagK] = new ModuleDef {
    make[AutoPublish[F]].from[VkAutoPublisher[F]]
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

  def ConfigModule = new ConfigModuleDef {
    makeConfig[Config]("conf")
  }

  def PublisherRole[F[_]: TagK] = new ModuleDef {
    make[PublisherRole[F]]
  }
}
