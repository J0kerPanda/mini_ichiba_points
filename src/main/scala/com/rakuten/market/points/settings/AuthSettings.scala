package com.rakuten.market.points.settings

import javax.crypto.spec.SecretKeySpec
import pureconfig.ConfigReader
import tsec.mac.jca.{HMACSHA256, MacSigningKey}

import scala.concurrent.duration.FiniteDuration

object AuthSettings {
  implicit val signingKeyReader: ConfigReader[MacSigningKey[HMACSHA256]] =
    ConfigReader[String].map(key => MacSigningKey[HMACSHA256](
      new SecretKeySpec(key.getBytes, "HS256")
    ))
}

case class AuthSettings(expirationInterval: FiniteDuration,
                        signingKey: MacSigningKey[HMACSHA256])
