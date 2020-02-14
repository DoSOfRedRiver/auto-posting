package dorr.modules.impl.events

import cats.effect.Sync
import cats.syntax.all._
import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.exceptions.LongPollServerTsException
import com.vk.api.sdk.objects.groups.LongPollServer

class VkLongPoll[F[_]: Sync](lp: LongPollServer, client: VkApiClient) extends LongPoll[F] {

  override def events(ts: Int) = Sync[F].delay {
    client.longPoll()
      .getEvents(lp.getServer, lp.getKey, ts)
      .execute()
      .some
  } recover {
    case _: LongPollServerTsException =>
      None
  }

  override def ts = lp.getTs.toInt.pure
}
