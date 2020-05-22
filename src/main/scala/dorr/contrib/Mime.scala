package dorr.contrib

import cats.Monad
import cats.instances.option._
import cats.syntax.traverse._
import com.twitter.io.Buf.ByteArray.Owned
import ru.tinkoff.tschema.finagle.{ParseBody, Rejection, Routed}
import tofu.syntax.monadic._

object Mime {
  case class AudioMpeg(bytes: Array[Byte])

  object AudioMpeg {
    val contentType = "audio/mpeg"

    implicit def parseBody[H[_]: Monad: Routed]: ParseBody[H, AudioMpeg] =
      new ParseBody[H, AudioMpeg] {
        override def parseOpt(): H[Option[AudioMpeg]] = {
          Routed.request.flatMap { req =>
            req.contentType.filter(_ == contentType) traverse { _ =>
              val bytes = Owned.extract(req.content)
              AudioMpeg(bytes).pure[H]
            }
          }
        }

        override def parse(): H[AudioMpeg] =
          parseOpt().flatMap {
            case Some(value) =>
              value.pure[H]
            case None =>
              Routed.reject(Rejection.body("Invalid content type"))
          }
      }
  }
}
