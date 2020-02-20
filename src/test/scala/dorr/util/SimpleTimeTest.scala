package dorr.util

import java.time.format.DateTimeParseException

import cats.Order
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.syntax.order._

class SimpleTimeTest extends AnyFlatSpec with Matchers {

  import SimpleTime._



  private def matchTime(str: String, hour: Int, minute: Int) = {
    fromString(str).toOption shouldEqual simpleTime(hour, minute)
  }

  "SimpleTime.fromString" should "return correct time for HH:00 pattern" in {
    matchTime("14:00", hour = 14, minute = 0)
  }

  it should "return correct time for 00:mm pattern" in {
    matchTime("00:15", hour = 0, minute = 15)
  }

  it should "return correct time for HH:mm pattern" in {
    matchTime("14:15", hour = 14, minute = 15)
  }

  it should "produce parse exception when invalid hour is specified" in {
    fromString("25:00").left.get.isInstanceOf[DateTimeParseException] shouldBe true
  }

  it should "produce parse exception when invalid minute is specified" in {
    fromString("21:75").left.get.isInstanceOf[DateTimeParseException] shouldBe true
  }

  import cats.instances.option._
  "SimpleTime's ordering" should "return a positive value" in {
    simpleTime(22, 0) compare simpleTime(21, 22)
  }

  it should "return a negative value" in {
    simpleTime(12, 32) compare simpleTime(21, 0)
  }

  it should "return zero" in {
    simpleTime(0, 0) compare simpleTime(0, 0)
  }
}

