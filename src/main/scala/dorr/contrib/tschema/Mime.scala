package dorr.contrib.tschema

import cats.Monad
import com.twitter.io.Buf.ByteArray.Owned
import ru.tinkoff.tschema.finagle.{ParseBody, Rejection, Routed}
import ru.tinkoff.tschema.swagger.{SwaggerPrimitive, SwaggerTypeable}
import tofu.syntax.monadic._
import cats.syntax.traverse._
import cats.instances.option._

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

    implicit val swaggerTypable = SwaggerTypeable.make[Mime.AudioMpeg](SwaggerPrimitive.bin(Mime.AudioMpeg.contentType))
  }
}
