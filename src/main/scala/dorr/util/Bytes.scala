package dorr.util

import org.apache.commons.lang3.SerializationUtils
import simulacrum.{op, typeclass}
import java.nio.ByteBuffer

@typeclass
trait Bytes[A] {
  @op("toBytes")   def to(a: A): Array[Byte]
  @op("fromBytes") def from(bytes: Array[Byte]): A
}

object Bytes {
  implicit val stringBytes = new Bytes[String] {
    override def to(a: String) = {
      a.getBytes("UTF-8")
    }

    override def from(bytes: Array[Byte]) = {
      new String(bytes, "UTF-8")
    }
  }

  implicit val intBytes: Bytes[Int] = new Bytes[Int] {
    override def to(a: Int): Array[Byte] =
      ByteBuffer.allocate(Integer.BYTES).putInt(a).array

    override def from(bytes: Array[Byte]): Int =
      ByteBuffer.wrap(bytes).getInt
  }

  def asSerializable[A <: Serializable]: Bytes[A] = new Bytes[A] {
    override def to(a: A): Array[Byte] =
      SerializationUtils.serialize(a)

    override def from(bytes: Array[Byte]): A =
      SerializationUtils.deserialize[A](bytes)
  }
}
