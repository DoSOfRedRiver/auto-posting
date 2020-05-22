package dorr.http

case class AuthData(userId: Long, serviceToken: String, providerToken: String, provider: String, expiresIn: Long)

object AuthData {
  //TODO could be derived automatically
  val tablePrefix = "auth-data"
}
