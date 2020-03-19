package dorr.modules.impl.events

import cats.effect.Sync
import cats.syntax.all._
import com.vk.api.sdk.client.{VkApiClient, actors}
import com.vk.api.sdk.client.actors.{GroupActor, ServiceActor, UserActor}
import com.vk.api.sdk.objects.enums.WallFilter
import com.vk.api.sdk.objects.wall.responses.GetResponse
import dorr.Configuration.Config

class VkApiImpl[F[_]: Sync](client: VkApiClient, conf: Config) extends VkApi[F] {
  def longPollServer = Sync[F].delay {
    val user = new GroupActor(conf.group.id, conf.group.secret)
    client.groupsLongPoll().getLongPollServer(user, conf.group.id).execute()
  }

  override def longPoll: F[LongPoll[F]] =
    longPollServer.map(new VkLongPoll[F](_, client))

  def suggestedPosts: F[GetResponse] = Sync[F].delay {
    client.wall()
      .get(new UserActor(conf.app.id, conf.accessToken))
      .filter(WallFilter.POSTPONED)
      .ownerId(-conf.group.id)
      .execute()
  }
}
