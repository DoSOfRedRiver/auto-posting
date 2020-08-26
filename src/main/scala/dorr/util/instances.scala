package dorr.util

import java.nio.ByteBuffer
import java.nio.file.{Files, Path, StandardOpenOption}
import java.time.{Instant, LocalDateTime}

import cats.effect.Sync

object instances {
  implicit def syncTime[F[_]: Sync]: Time[F] = new Time[F] {
    override def now: F[LocalDateTime] =
      Sync[F].delay(LocalDateTime.now())

    override def instant: F[Instant] =
      Sync[F].delay(Instant.now())
  }

  implicit def syncFile[F[_]: Sync]: File[F] =  new File[F] {
    override def write(path: Path, bytes: ByteBuffer): F[Unit] = {
      Sync[F].delay {
        val channel = Files.newByteChannel(path, StandardOpenOption.WRITE)
        channel.write(bytes)
        channel.close()
      }
    }

    override def create(path: Path): F[Unit] =
      Sync[F].delay(Files.createFile(path))
  }
}
