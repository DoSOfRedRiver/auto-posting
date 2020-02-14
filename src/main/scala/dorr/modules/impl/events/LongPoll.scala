package dorr.modules.impl.events

import com.vk.api.sdk.objects.callback.longpoll.responses.GetLongPollEventsResponse

trait LongPoll[F[_]] {
  def events(ts: Int): F[Option[GetLongPollEventsResponse]]
  def ts: F[Int]
}

object LongPoll {
  def apply[F[_]](implicit ev: LongPoll[F]): LongPoll[F] = ev
}