package dorr.util.storage

import cats.Functor
import cats.effect.Sync
import cats.syntax.all._
import dorr.util.Bytes
import dorr.util.syntax.mapT._
import org.rocksdb.RocksDB

class RocksStorage[F[_]: Sync](rocksDB: RocksDB) extends Storage[F, Array[Byte]] {

  def bytes(str: String) = str.getBytes("UTF-8")

  def put(key: String, value: Array[Byte]): F[Unit] = Sync[F].delay {
    rocksDB.put(bytes(key), value)
  }

  def get(key: String): F[Option[Array[Byte]]] = {
    Sync[F].delay(Option(rocksDB.get(bytes(key))))
  }
}

object RocksStorage {
  import dorr.util.Bytes.ops._

  class RocksGen[F[_]: Storage[*[_], Array[Byte]]: Functor, A: Bytes] extends Storage[F, A] {
    override def put(key: String, value: A) =
      Storage[F, Array[Byte]].put(key, value.toBytes)

    override def get(key: String) =
      Storage[F, Array[Byte]].get(key).mapT(Bytes[A].from)
  }

  class Impl[F[_]: RocksStorage: Functor, A: Bytes](F: Functor[F], rocks: RocksStorage[F]) extends Storage[F, A] {
    override def put(key: String, value: A) =
      rocks.put(key, value.toBytes)

    override def get(key: String) =
      rocks.get(key).mapT(Bytes[A].from)
  }

  def apply[F[_]](implicit ev: RocksStorage[F]): RocksStorage[F] = ev
}
