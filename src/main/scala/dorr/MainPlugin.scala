package dorr

import java.util.concurrent.Executors

import cats.data.OptionT
import cats.effect.{ConcurrentEffect, Resource, Sync, Timer}
import com.typesafe.config.ConfigFactory
import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.httpclient.HttpTransportClient
import distage.{ModuleDef, TagK}
import dorr.Main.Program
import dorr.modules.dsl.{Auth, Events, Publish}
import dorr.modules.impl._
import dorr.modules.impl.events.{VkApi, VkApiImpl, VkEvents}
import dorr.util.instances._
import dorr.util.{Time, modules}
import dorr.util.storage.RocksStorage.RocksGen
import dorr.util.storage.{RocksStorage, Storage}
import izumi.distage.config.{AppConfigModule, ConfigModuleDef}
import izumi.distage.model.definition.Axis
import izumi.distage.model.effect.DIEffectRunner
import izumi.distage.plugins.PluginDef
import izumi.logstage.api.IzLogger
import logstage.LogIO
import monix.eval.Task
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import org.rocksdb.{Options, RocksDB}

import scala.concurrent.ExecutionContext

object Authentication extends Axis {
  case object ConfigAuth extends AxisValueDef
  case object OAuth extends AxisValueDef
}

class MainPlugin extends PluginDef {
  implicit val sc = monix.execution.Scheduler.global

  val config = ConfigFactory.defaultApplication()
  val logger = IzLogger()

  implicit val log = LogIO.fromLogger[Task](logger)

  implicit val diEffectRunner = new DIEffectRunner[Task] {
    override def run[A](f: => Task[A]) = f.runSyncUnsafe()
  }

  def infrastructure[F[_]: TagK: LogIO: ConcurrentEffect: Timer] =
    modules.Misc ++ modules.DIEffects ++ AppConfigModule(config) ++ ConfigModule ++ PublisherRole ++ Rocks

  def implementations[F[_]: TagK: ConcurrentEffect: LogIO: Timer] =
    Program ++  Modules ++ MainFunctionalModule ++ Client ++ Http4s ++ Storage ++ Instruments

  include(implementations[Task] ++ infrastructure[Task])


  def Instruments[F[_]: TagK: Time] = new ModuleDef {
    addImplicit[Time[F]]
  }


  def Modules[F[_] : TagK : Sync] = new ModuleDef {
    make[Publish[F]].from[VkPublish[F]]
    make[Events[F]].from[VkEvents[F]]
    make[VkApi[F]].from[VkApiImpl[F]]

    make[Auth[F]].tagged(Authentication.OAuth)
      .from[VkOAuth[F]]
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

  //TODO remove
  def Program[F[_] : TagK] = new ModuleDef {
    make[Program[F]]
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

  def Http4s[F[_] : TagK : ConcurrentEffect : Timer] = new ModuleDef {

    make[BlazeClientBuilder[F]].from {
      val executor = Executors.newCachedThreadPool()
      val ec = ExecutionContext.fromExecutor(executor)
      BlazeClientBuilder[F](ec)
    }

    make[BlazeServerBuilder[F]].from { (config: Config) =>
      BlazeServerBuilder[F]
        .bindHttp(config.oauth.serverPort, config.oauth.serverAddr)
        .withNio2(true)
    }
  }

  def ConfigModule = new ConfigModuleDef {
    makeConfig[Config]("conf")
  }

  def PublisherRole[F[_]: TagK] = new ModuleDef {
    make[PublisherRole[F]]
  }
}
