package dorr

import java.time.ZoneOffset

case class Database(path: String)
case class App(id: Int, secret: String)
case class Group(id: Int, secret: String)
case class OAuth(redirectAddr: String, serverAddr: String, serverPort: Int)
case class Schedule(postingPoints: List[String], daysBeforeQueueing: Int, timezoneOffset: ZoneOffset)
case class Config(accessToken: String, group: Group, app: App, oauth: OAuth, database: Database, schedule: Schedule)