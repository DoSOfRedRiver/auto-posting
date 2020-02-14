package dorr.modules.impl

import java.time.{LocalDateTime, ZoneId, ZoneOffset}

import cats.effect.Sync
import cats.syntax.all._
import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.{GroupActor, UserActor}
import dorr.Config
import dorr.modules.dsl.{Message, Publish}
import logstage.LogIO

import scala.jdk.CollectionConverters._

class VkPublish[F[_]: Sync: LogIO](client: VkApiClient, conf: Config) extends Publish[F] {

  override def publish(msg: Message): F[Unit] = {
    for {
      resp  <- publishMessage(msg)
      _     <- LogIO[F].info(s"Published $msg; response $resp")
    } yield ()
  }

  def publishMessage(msg: Message) = {
    Sync[F].delay {
      //TODO repalce with schedule
      val date = LocalDateTime
        .now()
        .plusHours(1)
        .toEpochSecond(conf.schedule.timezoneOffset).toInt

      client.wall()
        .post(new UserActor(conf.app.id, conf.accessToken))
        .ownerId(-conf.group.id)
        .fromGroup(true)
        .message(msg.text)
        .attachments(msg.attachments.asJava)
        .publishDate(
          date
        )
        .execute()
    }
  }
}
