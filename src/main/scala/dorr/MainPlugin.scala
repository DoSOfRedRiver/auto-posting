package dorr

import java.time.format.DateTimeFormatter

import cats.data.OptionT
import cats.effect.{ConcurrentEffect, Resource, Sync, Timer}
import cats.instances.list._
import cats.syntax.foldable._
import cats.{Applicative, Apply, Monad}
import com.twitter.finagle.http.Response
import com.twitter.util.Future
import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.httpclient.HttpTransportClient
import distage.{DIKey, ModuleDef, TagK}
import dorr.Configuration.Config
import dorr.Main.Program
import dorr.http.{Routes, UploadHandler}
import dorr.initializers.{BackgroundProcess, HttpServerInit}
import dorr.modules.dsl.{Auth, Events, Publish, Schedule}
import dorr.modules.impl._
import dorr.modules.impl.events.{VkApi, VkApiImpl, VkEvents}
import dorr.util.instances._
import dorr.util.storage.RocksStorage.RocksGen
import dorr.util.storage.{RocksStorage, Storage}
import dorr.util.{File, Time, modules}
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
import ru.tinkoff.tschema.finagle.envRouting.TaskRouting.TaskHttp
import ru.tinkoff.tschema.finagle.{LiftHttp, MkService, RoutedPlus, RunHttp}
import tofu.BracketThrow
import tofu.lift.Lift
import tofu.syntax.monadic._

object Authentication extends Axis {
  case object ConfigAuth extends AxisValueDef
  case object OAuth extends AxisValueDef
}

class MainPlugin extends PluginDef {
  implicit val sc = monix.execution.Scheduler.global

  val logger = IzLogger()

  implicit val log = LogIO.fromLogger[Task](logger)


  implicit val localDateConvert = localTimeConfigConvert(DateTimeFormatter.ISO_TIME)
  implicit val configReader: pureconfig.ConfigReader[Config] = PureconfigAutoDerive[Config]

  implicit val diEffectRunner = new DIEffectRunner[Task] {
    override def run[A](f: => Task[A]) = f.runSyncUnsafe()
  }

  implicit val liftTwitterFutureToTask = new Lift[Future, Task] {
    override def lift[A](fa: Future[A]): Task[A] = Task.async { clb =>
      fa.respond(x => clb(x.asScala.toEither))
    }
  }

  val backgroundProcessesKey = DIKey.get[Set[Unit]].named("background-tasks")

  def infrastructure[F[_]: TagK: LogIO: ConcurrentEffect: Timer] =
    modules.Misc ++ modules.DIEffects ++ ConfigModule ++ PublisherRole ++ Rocks ++ HttpServer[TaskHttp, Task]

  def implementations[F[_]: TagK: ConcurrentEffect: LogIO: Timer] =
    Program ++ Modules ++ MainFunctionalModule ++ Client ++ Storage ++ Instruments ++ HttpHandlers ++ HttpRoutes[Task, TaskHttp]

  include(implementations[Task] ++ infrastructure[Task])


  def HttpHandlers[F[_]: TagK] = new ModuleDef {
    make[UploadHandler[F]]
  }

  def HttpRoutes[F[_]: Applicative: TagK, H[_]: TagK: Monad: LiftHttp[*[_], F]: RoutedPlus] = new ModuleDef {
    import ru.tinkoff.tschema.finagle.circeInstances._

    //DOES NOT COMPILE WITHOUT MONAD INSTANCE FOR H
    many[H[Response]].add((uploadHandler: UploadHandler[F]) =>
      MkService[H](Routes.upload)(uploadHandler)
    )

    many[H[Response]].add(MkService[H](Routes.status)(new {
      def status: F[String] = "Alive".pure[F]
    }))
  }

  def HttpServer[H[_]: TagK: RoutedPlus: BracketThrow, F[_]: TagK](
    implicit R: RunHttp[H, F], L: Lift[F, H]
  ) = new ModuleDef {
    addImplicit[Lift[F, H]]
    addImplicit[RunHttp[H, F]]
    addImplicit[RoutedPlus[H]]
    addImplicit[BracketThrow[H]]
    many[BackgroundProcess[F]].add[HttpServerInit[H, F]]
  }

  def Instruments[F[_]: TagK: Time: File] = new ModuleDef {
    addImplicit[Time[F]]
    addImplicit[File[F]]
  }

  def Modules[F[_] : TagK : Sync] = new ModuleDef {
    make[Publish[F]].from[VkPublish[F]]
    make[Events[F]].from[VkEvents[F]]
    make[VkApi[F]].from[VkApiImpl[F]]
    make[Schedule[F]].from[VkSchedule[F]]

    make[Auth[F]].tagged(Authentication.ConfigAuth)
      .from[VkConfigAuth[F]]
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
