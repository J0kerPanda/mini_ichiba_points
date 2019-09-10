package com.rakuten.market.points.settings

case class PointsSettings(transaction: PointsTransactionSettings,
                          expiring: ExpiringPointsSettings)
