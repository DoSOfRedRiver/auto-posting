package dorr.http

import cats.Apply
import dorr.Configuration.Config
import dorr.contrib.tschema.Mime
import dorr.util.File
import tofu.syntax.monadic._

class UploadHandler[F[_]: File: Apply](config: Config) {
  def upload(body: Mime.AudioMpeg): F[String] = {
    //TODO path resolution by request id
    val path = config.mediaDir.resolve("temp.mp3")
    File[F].create(path) *> File[F].write(path, body.bytes) as "Ok!"
  }
}
