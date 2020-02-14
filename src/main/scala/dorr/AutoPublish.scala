package dorr

trait AutoPublish[F[_]] {
  def run: F[Unit]
}

object AutoPublish {
  def apply[F[_]](implicit ev: AutoPublish[F]): AutoPublish[F] = ev
}
