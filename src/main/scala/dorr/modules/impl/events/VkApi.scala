package dorr.modules.impl.events

import com.vk.api.sdk.objects.wall.responses.GetResponse

trait VkApi[F[_]] {
  def longPoll: F[LongPoll[F]]
  def suggestedPosts: F[GetResponse]
}

object VkApi {
  def apply[F[_]](implicit ev: VkApi[F]): VkApi[F] = ev
}
