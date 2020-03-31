package dorr

import cats.Monad
import cats.effect.Timer
import cats.instances.list._
import cats.instances.option._
import cats.syntax.all._
import dorr.Configuration.Config
import dorr.modules.dsl._
import logstage._

import scala.concurrent.duration._

class VkAutoPublisher[F[_]: Monad: LogIO: Events: Publish: Schedule: Timer](conf: Config) extends AutoPublish[F] {

  val tick = for {
    events  <- Events[F].events
    _       <- publishEvents(events)
  } yield ()

  def run: F[Unit] =
    for {
      _ <- LogIO[F].info("Next pull")
      _ <- tick
      _ <- Timer[F].sleep(conf.idlePeriod.seconds)
      _ <- run
    } yield ()

  //TODO factor out?
  def publishEvents(events: List[Event]): F[Unit] = {
    for {
      _       <- LogIO[F].info(s"Events size: ${events.size}")
      messages = events.map(_.`object`.attachments
        .headOption
        .map(attachment =>
          Message("New:", List(s"audio${attachment.audio.ownerId}_${attachment.audio.id}"))
        )).unite

      dates   <- Schedule[F].nextDates(messages.size)
      _       <- (messages zip dates) traverse (Publish[F].publish _).tupled
    } yield ()
  }
}
