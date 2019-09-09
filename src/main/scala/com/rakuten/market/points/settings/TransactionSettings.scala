package com.rakuten.market.points.settings

import scala.concurrent.duration.FiniteDuration

case class TransactionSettings(expirationInterval: FiniteDuration,
                               checkEvery: FiniteDuration)
