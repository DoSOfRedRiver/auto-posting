package dorr.modules.impl.events

import cats.Monad
import cats.instances.list._
import cats.syntax.all._
import com.google.gson.JsonObject
import com.vk.api.sdk.objects.callback.longpoll.responses.GetLongPollEventsResponse
import dorr.modules.dsl.{Event, Events}
import dorr.util.storage.Storage
import io.circe.parser.decode
import logstage.LogIO

import scala.jdk.CollectionConverters._

class VkEvents[F[_] : Monad : VkApi : LogIO : Storage[*[_], Int]] extends Events[F] {

  def currentTs(newTs: Int): F[Int] =
    Storage[F, Int].get("ts").map(_.getOrElse(newTs))

  def handleResponse(maybeResponse: Option[GetLongPollEventsResponse]): F[List[Event]] = {
    maybeResponse match {
      case Some(response) =>
        val ts = response.getTs
        val updates = response.getUpdates.asScala.toList

        for {
          _ <- Storage[F, Int].put("ts", ts)
          //TODO rollback strategy?
          _ <- LogIO[F].info(s"Updated ts value is: $ts")
          r <- transformEvents(updates)
        } yield r
      case None =>
        LogIO[F]
          .error("Wrong ts value has been used.")
          .as(List.empty)
    }
  }

  def transformEvents(updates: List[JsonObject]): F[List[Event]] = {
    val parsed = updates.map(jo => decode[Event](jo.toString))

    val events = parsed.foldl(List.empty[Event].pure[F]) { (list, e) =>
      e match {
        case Left(err) =>
          list <* LogIO[F].error(s"An error occurred during parsing an update value: $err")
        case Right(value) =>
          list.map(value :: _)
      }
    }

    events.map(_.reverse)
  }

  def requestEvents: F[Option[GetLongPollEventsResponse]] = {
    for {
      server  <- VkApi[F].longPoll
      ts      <- server.ts >>= currentTs
      resp    <- server.events(ts)
    } yield resp
  }

  override def events: F[List[Event]] = requestEvents >>= handleResponse
}
