package dorr.util

import org.scalacheck.Prop.forAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.Checkers

class BytesTest extends AnyFlatSpec with Checkers {
  "Bytes[Int]" should "correctly deserialize serialized Int" in {
    check(
      forAll { input: Int =>
        val bytes = Bytes[Int] to input
        (Bytes[Int] from bytes) == input
      }
    )
  }
}
