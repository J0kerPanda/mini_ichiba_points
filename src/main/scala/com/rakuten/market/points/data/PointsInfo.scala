package com.rakuten.market.points.data

case class PointsInfo(total: Points.Amount,
                      totalExpiring: Option[Points.Amount],
                      closestExpiring: Option[ExpiringPoints])
