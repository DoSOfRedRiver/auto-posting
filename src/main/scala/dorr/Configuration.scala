package dorr

import java.nio.file.Path
import java.time.{LocalTime, ZoneOffset}

object Configuration {
  case class Database(path: String)
  case class App(id: Int, secret: String)
  case class Group(id: Int, secret: String)
  case class OAuth(redirectAddr: String, serverAddr: String, serverPort: Int)
  case class Schedule(postingPoints: List[LocalTime], queueInDays: Int, timezoneOffset: ZoneOffset, clashInterval: Int)
  case class Config(httpAddr: String, mediaDir: Path, accessToken: String, idlePeriod: Int, group: Group, app: App, oauth: OAuth, database: Database, schedule: Schedule)
}