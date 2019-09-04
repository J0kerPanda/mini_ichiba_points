package com.rakuten.market.points.data

object PointsInfo {

  def empty(userId: UserId): PointsInfo =
    PointsInfo(userId, 0, 0, None)
}

case class PointsInfo(userId: UserId,
                      total: Points.Amount,
                      totalExpiring: Points.Amount,
                      closestExpiring: Option[Points.Expiring])
