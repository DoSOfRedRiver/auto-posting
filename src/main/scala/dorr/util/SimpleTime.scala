package dorr.util

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoField, TemporalAccessor}

import cats.Order
import cats.syntax.option._
import cats.syntax.either._
import io.circe.Decoder

sealed abstract case class SimpleTime(hour: Int, minute: Int) {
  def toLocalTime: LocalTime = LocalTime.of(hour, minute)
}

private class HackToAvoidCirceShittyMacro(hour: Int, minute: Int) extends SimpleTime(hour, minute)

object SimpleTime {
  val formatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm")

  def simpleTime(hour: Int, minute: Int): Option[SimpleTime] = {
    if (hour <= 24 && minute <= 60 && hour >= 0 && minute >= 0)
      new HackToAvoidCirceShittyMacro(hour, minute).some
    else None
  }

  def fromString(str: String): Either[Throwable, SimpleTime] = {
    Either.catchNonFatal {
      formatter.parse(str)
    } map fromTemporal
  }

  def fromTemporal(temporal: TemporalAccessor): SimpleTime = {
    new HackToAvoidCirceShittyMacro(
      temporal.get(ChronoField.HOUR_OF_DAY),
      temporal.get(ChronoField.MINUTE_OF_HOUR)
    )
  }

  implicit val simpleTimeOrdering: Ordering[SimpleTime] = {
    case (SimpleTime(h1, m1), SimpleTime(h2, m2)) =>
      (h1 + m1 * 60) - (h2 + m2 * 60)
  }

  implicit val simpleTimeOrder: Order[SimpleTime] = Order.fromOrdering

  implicit val circeDecoder: Decoder[SimpleTime] =
    Decoder.decodeLocalTimeWithFormatter(formatter)
      .map(fromTemporal)
}