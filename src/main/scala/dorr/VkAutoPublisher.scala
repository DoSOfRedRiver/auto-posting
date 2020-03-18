package dorr

import cats.Monad
import cats.instances.list._
import cats.syntax.all._
import dorr.modules.dsl.{Auth, Events, Message, Publish, Schedule}
import logstage._

class VkAutoPublisher[F[_]: Monad: LogIO: Events: Publish: Auth: Schedule] extends AutoPublish[F] {


  def run: F[Unit] = {

    for {
      events  <- Events[F].events
      _       <- LogIO[F].info(s"Events size: ${events.size}")
      messages = events map { event =>
        //TODO head
        val ownerId = event.`object`.attachments.head.audio.ownerId
        val mediaId = event.`object`.attachments.head.audio.id
        Message("New:", List(s"audio${ownerId}_${mediaId}"))
      }
      dates   <- Schedule[F].nextDates(messages.size)
      _       <- (messages zip dates) traverse (Publish[F].publish _).tupled
    } yield ()


  }
}
