package dorr.modules.impl

import java.time._

import cats.Monad
import cats.data.StateT
import cats.instances.list._
import cats.instances.option._
import cats.syntax.foldable._
import cats.syntax.option._
import cats.syntax.traverse._
import com.vk.api.sdk.objects.wall.responses.GetResponse
import dorr.Configuration.Config
import dorr.modules.dsl.Schedule
import dorr.modules.impl.events.VkApi
import dorr.util.Time
import tofu.optics.chain
import tofu.optics.macros.GenContains
import tofu.optics.tags.at
import tofu.syntax.monadic._

import scala.jdk.CollectionConverters._


class VkSchedule[F[_] : VkApi : Time : Monad](conf: Config) extends Schedule[F] { sc =>
  type Days = Map[Int, List[LocalTime]]

  case class ScheduleState(days: Days, nextDay: Int, daysRemap: IndexedSeq[Int]) {
    def addSlot(day: Int, slot: LocalTime): ScheduleState = {
      chain(this) >> sc.days > at >@ day update (op => (slot :: op.foldK).some)
    }

    def eraseDay(dayIndex: Int): ScheduleState = {
      val k = daysRemap indexOf dayIndex

      if (k < 0) throw new IllegalStateException("Inconsistent state between 'days' and 'daysRemap'")

      val remap = daysRemap.patch(k, Nil, 1) :+ nextDay

      val days = this.days.removed(dayIndex)

      copy(days = days, daysRemap = remap, nextDay = nextDay + 1)
    }
  }

  val dateOrdering = implicitly[Ordering[LocalTime]]
  import dateOrdering._

  val days = GenContains[ScheduleState](_.days)
  val daysRemap = GenContains[ScheduleState](_.daysRemap)
  val daysBeforeQueueing = conf.schedule.daysBeforeQueueing

  val postingPoints = conf.schedule.postingPoints
    .distinct
    .toIndexedSeq

  def now: F[ZonedDateTime] =
    Time[F].now map (_.atZone(conf.schedule.timezoneOffset))

  def restOfTheDay: F[IndexedSeq[LocalTime]] =
    now map { now => postingPoints.filter(_ > now.toLocalTime) }

  def occupiedPoints(resp: GetResponse): Days = {
    resp.getItems.asScala.toList.map { item =>
      val instant = Instant.ofEpochSecond(item.getDate.toLong)
      LocalDateTime.ofInstant(instant, conf.schedule.timezoneOffset)
    }.groupBy(_.getDayOfYear) transform { case (_, l) => l.map(_.toLocalTime) }
  }

  def timeSlots(response: GetResponse, points: IndexedSeq[LocalTime], today: LocalDateTime, slots: Int): F[List[LocalDateTime]] = {
    val occupiedDays = occupiedPoints(response)
    val dayOfYear = today.getDayOfYear

    def tryDay(occupiedInDay: List[LocalTime], point: LocalTime): Option[LocalTime] = {
      Some(point).filter(point =>
        occupiedInDay.forall(
          Duration.between(_, point).toMinutes.abs > conf.schedule.clashInterval
        )
      )
    }

    def setDate(value: LocalTime, day: Int): StateT[F, ScheduleState, LocalDateTime] = {
      val dayToOccupy = LocalTime.from(value)
      val res = dayToOccupy
        .atDate(today.toLocalDate)
        .withDayOfYear(1)
        .plusDays(day - 1)

      StateT.modify[F, ScheduleState](_.addSlot(day, dayToOccupy)) as res
    }

    def recalcState: StateT[F, ScheduleState, Unit] = {
      for {
        st  <- StateT.get[F, ScheduleState]
        _   <- st.days.toList.traverse_ { case (ind, l) =>
          StateT.modify[F, ScheduleState](_.eraseDay(ind))
            .whenA(postingPoints.forall(l.contains))
        }
      } yield ()
    }

    def oneSlot(c: Int): StateT[F, ScheduleState, LocalDateTime] = {
      val postingPoint = points((c / daysBeforeQueueing) % points.size)
      val dayDist = c % daysBeforeQueueing

      for {
        dayToCheck    <- StateT.inspect[F, ScheduleState, Int](_.daysRemap(dayDist))
        occupiedInDay <- StateT.inspect[F, ScheduleState, List[LocalTime]](_.days.getOrElse(dayToCheck, List.empty))
        step          =  tryDay(occupiedInDay, postingPoint) map (setDate(_, dayToCheck) <* recalcState)
        res           <- step getOrElse oneSlot(c + 1)
      } yield res
    }

    val state = ScheduleState(
      days = occupiedDays,
      nextDay = dayOfYear + daysBeforeQueueing,
      daysRemap = IndexedSeq.range(dayOfYear, dayOfYear + daysBeforeQueueing)
    )

    (0 until slots).toList.traverse(_ => oneSlot(0)).runA(state)
  }

  override def nextDates(n: Int): F[List[LocalDateTime]] = {
    for {
      restOfTheDay  <- restOfTheDay
      now           <- now
      posts         <- VkApi[F].suggestedPosts
      res           <- timeSlots(posts, restOfTheDay, now.toLocalDateTime, n)
    } yield res
  }
}
