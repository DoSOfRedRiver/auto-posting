package dorr.util

import simulacrum.{op, typeclass}

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
}
