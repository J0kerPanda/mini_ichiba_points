package com.rakuten.market.points.data

object PointsInfo {

  def empty(userId: UserId): PointsInfo =
    PointsInfo(userId, 0, None, None)
}

case class PointsInfo(userId: UserId,
                      total: Points.Amount,
                      totalExpiring: Option[Points.Amount],
                      closestExpiring: Option[Points.Expiring])
