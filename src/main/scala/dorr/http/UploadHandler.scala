package dorr.http

import cats.Monad
import dorr.Configuration.Config
import dorr.contrib.tschema.Mime
import dorr.initializers.HasSession
import dorr.modules.defs.Session
import dorr.util.File
import ru.tinkoff.tschema.finagle.LiftHttp
import tofu.syntax.monadic._

trait UploadHandler[F[_], H[_]] {
  implicit val file: File[F]
  implicit val MF: Monad[F]
  implicit val MH: Monad[H]
  implicit val LH: LiftHttp[H, F]
  implicit val config: Config
  implicit val sessionDep: HasSession[H, F]

  def upload(body: Mime.AudioMpeg): H[String] = {
    for {
      session <- sessionDep.context
      _       <- session match {
        case Some(session) =>
          LH(save(body, session))
        case None =>
          throw new IllegalStateException("No session found!")
      }
    } yield "Ok!"
  }

  def save(body: Mime.AudioMpeg, session: Session): F[Unit] = {
    val path = config.mediaDir.resolve(s"${session.userId}.mp3")

    for {
      _ <- File[F].create(path)
      _ <- File[F].write(path, body.bytes)
    } yield ()
  }
}
