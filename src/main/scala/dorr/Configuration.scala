package dorr

import java.nio.file.Path
import java.time.{LocalTime, ZoneOffset}

object Configuration {
  case class Database(path: String)
  case class Security(csrfLifetime: Long)
  case class App(id: Int, secret: String)
  case class Group(id: Int, secret: String)
  case class OAuth(authorizationUrl: String, tokenUrl: String, clbAddr: String)
  case class Schedule(postingPoints: List[LocalTime], queueInDays: Int, timezoneOffset: ZoneOffset, clashInterval: Int)
  case class Config(httpAddr: String, mediaDir: Path, accessToken: String, idlePeriod: Int, group: Group, app: App, oauth: OAuth, database: Database, schedule: Schedule, security: Security)
}