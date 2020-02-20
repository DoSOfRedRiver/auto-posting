package dorr.modules.impl

import java.time.temporal.ChronoUnit.MINUTES
import java.time.{Instant, LocalDateTime, LocalTime, ZonedDateTime}

import cats.Monad
import cats.syntax.all._
import com.vk.api.sdk.objects.wall.responses.GetResponse
import dorr.Config
import dorr.modules.dsl.Schedule
import dorr.modules.impl.events.VkApi
import dorr.util.{SimpleTime, Time}

import scala.jdk.CollectionConverters._


class VkSchedule[F[_] : VkApi : Time : Monad](conf: Config) extends Schedule[F] {
  private val postingPoints = conf.schedule.postingPoints
    .distinct

  def now: F[ZonedDateTime] =
    Time[F].now map (_.atZone(conf.schedule.timezoneOffset))

  def restOfTheDay: F[List[SimpleTime]] =
    now map { now => postingPoints.filter(_ < SimpleTime.fromTemporal(now)) }

  def occupiedPoints(resp: GetResponse): Map[Int, List[LocalDateTime]] = {
    resp.getItems.asScala.toList.map { item =>
      val instant = Instant.ofEpochSecond(item.getDate.toLong)
      LocalDateTime.from(instant)
    }.groupBy(_.getDayOfYear)
  }

  def timeSlot(response: GetResponse, points: List[SimpleTime], today: Int): LocalDateTime = {
    val ltPoints = points.map(_.toLocalTime)
    val occupiedInDay = occupiedPoints(response)

    def tryDay(occupiedInDay: List[LocalDateTime]): Option[LocalTime] = {
      ltPoints.find { point =>
        occupiedInDay.forall(_.until(point, MINUTES) > conf.schedule.clashInterval)
      }
    }

    def step(c: Int): LocalDateTime = {
      if (c <= conf.schedule.daysBeforeQueueing) {
        val lt = tryDay(occupiedInDay(c))
          .getOrElse(step(c + 1))

        LocalDateTime.from(lt)
          .withDayOfYear(today)
          .plusDays(c)
      } else
        //TODO decide about the strategy when no more slots left
        throw new IllegalStateException("Ooops")
      }

    step(0)
  }

  override def nextDate: F[LocalDateTime] = {
    for {
      restOfTheDay  <- restOfTheDay
      now           <- now
      posts         <- VkApi[F].suggestedPosts
    } yield timeSlot(posts, restOfTheDay, now.getDayOfYear)
  }
}
