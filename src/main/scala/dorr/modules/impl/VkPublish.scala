package dorr.modules.impl

import java.time.LocalDateTime

import cats.effect.Sync
import cats.syntax.all._
import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.UserActor
import com.vk.api.sdk.objects.wall.responses.PostResponse
import dorr.Configuration.Config
import dorr.modules.dsl.{Message, Publish, Schedule}
import logstage.LogIO

import scala.jdk.CollectionConverters._

class VkPublish[F[_]: Sync: LogIO](client: VkApiClient, conf: Config) extends Publish[F] {

  override def publish(msg: Message, date: LocalDateTime): F[Unit] = {
    for {
      resp  <- publishMessage(msg, date)
      _     <- LogIO[F].info(s"Published $msg; response $resp")
    } yield ()
  }

  def publishMessage(msg: Message, date: LocalDateTime): F[PostResponse] = {
    Sync[F].delay {
      client.wall()
        .post(new UserActor(conf.app.id, conf.accessToken))
        .ownerId(-conf.group.id)
        .fromGroup(true)
        .message(msg.text)
        .attachments(msg.attachments.asJava)
        .publishDate(
          date.toEpochSecond(conf.schedule.timezoneOffset).toInt
        )
        .execute()
    }
  }
}
