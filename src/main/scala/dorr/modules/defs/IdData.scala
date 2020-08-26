package dorr.modules.defs

case class IdData(userId: Long, providerToken: String, provider: String, expiresIn: Long)

object IdData {
  //TODO could be derived automatically
  val tablePrefix = "auth-data"
}
