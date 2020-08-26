package dorr.util

import cats.effect.Sync
import tsec.mac.MAC
import tsec.mac.jca.{HMACSHA256, MacSigningKey}

trait Crypto[F[_]] {
  type Key = MacSigningKey[HMACSHA256]
  type Data = Array[Byte]

  def verify(key: Key, mac: Data, data: Data): F[Boolean]

  def hmac(key: Key, data: Data): F[Data]

  def key: F[Key]
}

object Crypto {
  def apply[F[_]: Crypto]: Crypto[F] = implicitly

  def tsecSyncCrypto[F[_] : Sync]: Crypto[F] =
    new Crypto[F] {
      def verify(key: Key, mac: Data, data: Data): F[Boolean] =
        HMACSHA256.verifyBool[F](data, MAC(mac), key)

      def hmac(key: Key, data: Data): F[Data] =
        MAC.unsubst(HMACSHA256.sign[F](data, key))

      def key: F[Key] = HMACSHA256.generateKey[F]
    }
}
