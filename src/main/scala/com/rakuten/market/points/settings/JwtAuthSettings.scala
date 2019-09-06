package com.rakuten.market.points.settings

import tsec.mac.jca.{HMACSHA256, MacSigningKey}

import scala.concurrent.duration.FiniteDuration

case class JwtAuthSettings(expiryDuration: FiniteDuration,
                           signingKey: MacSigningKey[HMACSHA256])
