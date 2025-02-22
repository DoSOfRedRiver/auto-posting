package dorr.modules.dsl

import java.time.LocalDateTime

case class Message(text: String, attachments: List[String])

trait Publish[F[_]] {
  def publish(msg: Message, date: LocalDateTime): F[Unit]
}

object Publish {
  def apply[F[_]](implicit ev: Publish[F]): Publish[F] = ev
}
