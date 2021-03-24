package dorr.util

import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.nio.file.{Path, Paths}

trait File[F[_]] {
  def write(path: Path, bytes: ByteBuffer): F[Unit]

  def create(path: Path): F[Unit]

  def read(path: Path): F[SeekableByteChannel]

  def readAll(path: Path): F[Array[Byte]]

  def write(path: Path, bytes: Array[Byte]): F[Unit] =
    write(path, ByteBuffer.wrap(bytes))

  def write(name: String, bytes: ByteBuffer): F[Unit] =
    write(Paths.get(name), bytes)

  def write(name: String, bytes: Array[Byte]): F[Unit] =
    write(Paths.get(name), bytes)
}

object File {
  def apply[F[_]](implicit ev: File[F]): File[F] = ev
}
