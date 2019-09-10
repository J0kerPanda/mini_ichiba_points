package com.rakuten.market.points.settings

import scala.concurrent.duration.FiniteDuration

case class PointsTransactionSettings(expirationInterval: FiniteDuration,
                                     checkEvery: FiniteDuration)
