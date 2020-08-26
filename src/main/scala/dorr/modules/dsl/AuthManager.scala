package dorr.modules.dsl

import dorr.modules.defs.{IdData, SessionData}

trait AuthManager[F[_]] {
  def authenticate(data: IdData): F[SessionData]
  def authorize(sessionData: SessionData): F[String]
}

object AuthManager {
  def apply[F[_]: AuthManager]: AuthManager[F] = implicitly
}
