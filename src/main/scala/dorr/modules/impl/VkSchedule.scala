package dorr.modules.impl

import java.time.{LocalDateTime, ZoneId}

import cats.effect.Sync
import cats.syntax.all._
import cats.instances.list._
import dorr.Config
import dorr.modules.dsl.Schedule
import dorr.modules.impl.events.VkApi

import scala.jdk.CollectionConverters._

class VkSchedule[F[_]: VkApi: Sync](conf: Config) extends Schedule[F] {
  override def nextDate = {
    val now =
      LocalDateTime
        .now()
        .atZone(conf.schedule.timezoneOffset)

    for {
      posts <- VkApi[F].suggestedPosts
      _ <- posts.getItems.asScala.toList.traverse { item =>
        Sync[F].delay {
          item.getDate
        }
      }
    } yield 4
  }
}
