package dorr.modules.dsl

case class Audio(id: Int, ownerId: Int)
case class Attachment(audio: Audio)
case class Obj(topicId: Int, text: String, fromId: Int, attachments: List[Attachment])
case class Event(`type`: String, eventId: String, `object`: Obj)

trait Events[F[_]] {
  def events: F[List[Event]]
}

object Events {
  def apply[F[_]](implicit ev: Events[F]) = ev
}
