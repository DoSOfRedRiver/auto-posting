package dorr

import java.time.{LocalDateTime, LocalTime}

import cats.syntax.all._
import com.vk.api.sdk.objects.wall.WallpostFull
import com.vk.api.sdk.objects.wall.responses.GetResponse
import dorr.Configuration.Config
import dorr.modules.dsl.Schedule
import dorr.modules.impl.events.{LongPoll, VkApi}
import dorr.testutils.TimeDummy
import dorr.util.Time
import izumi.distage.model.definition.ModuleDef
import izumi.distage.plugins.PluginConfig
import izumi.distage.testkit.TestConfig
import izumi.distage.testkit.scalatest.DistageSpecScalatest
import monix.eval.Task

import scala.jdk.CollectionConverters._

class SchedulerTest extends DistageSpecScalatest[Task] {
  type Now = LocalDateTime

  class VkApiDummy(config: Config, now: Now) extends VkApi[Task] {
    override def longPoll: Task[LongPoll[Task]] = ???

    val slot1: LocalTime = config.schedule.postingPoints(0)
    val slot2: LocalTime = config.schedule.postingPoints(1)

    val timepoints = List(
      now `with` slot1,
      now `with` slot2,
      now `with` slot1 plusDays 1,
      now,
    )

    override def suggestedPosts: Task[GetResponse] = {
      val response = new GetResponse()
        .setItems(
          timepoints
            .map(_.toEpochSecond(config.schedule.timezoneOffset))
            .map { time =>
              val post = new WallpostFull
              post.setDate(time.toInt)
              post
            }
            .asJava
        )

      response.pure[Task]
    }
  }

  def assertTask(expr: => Boolean): Task[Unit] = Task.delay {
    assert(expr)
  }

  val schedulerOverrides = new ModuleDef {
    make[Now] from {
      LocalDateTime.of(1990, 1, 2, 0, 0)
    }
    make[Time[Task]] from { now: Now => new TimeDummy[Task](now) }
    make[VkApi[Task]].from[VkApiDummy]
  }

  override protected def config: TestConfig = super.config.copy(
    pluginConfig = PluginConfig.const(new MainPlugin),
    moduleOverrides = schedulerOverrides
  )

  "Schedule" should {

    "return correct slots when some of them occupied" in { (schedule: Schedule[Task], now: Now, config: Config) =>
      val slot1: LocalTime = config.schedule.postingPoints(0)
      val slot2: LocalTime = config.schedule.postingPoints(1)
      val slot3: LocalTime = config.schedule.postingPoints(2)

      val slots = List(
        now `with` slot1 plusDays 2,
        now `with` slot1 plusDays 3,
        now `with` slot1 plusDays 4,
        now `with` slot2 plusDays 1,
        now `with` slot2 plusDays 2,
        now `with` slot2 plusDays 3,
        now `with` slot2 plusDays 4,
        now `with` slot3,
        now `with` slot1 plusDays 5,
        now `with` slot2 plusDays 5,
        now `with` slot3 plusDays 1,
        now `with` slot1 plusDays 6,
      )
      for {
        dates <- schedule nextDates slots.size
        _     =  assert(dates == slots)
      } yield ()
    }

    "return empty list when zero slots required" in { (schedule: Schedule[Task]) =>
      for {
        dates <- schedule nextDates 0
        _     =  assert(dates == List.empty)
      } yield ()
    }

    "return list without duplicates when a lot of dates required" in { (schedule: Schedule[Task]) =>
      val n = 500
      for {
        dates <- schedule nextDates n
        _     =  assert(dates.distinct.length == n)
      } yield ()
    }
  }
}
