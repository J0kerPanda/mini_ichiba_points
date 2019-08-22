package com.rakuten.market.points.algebra.core

import com.rakuten.market.points.data.{ExpiringPoints, PointsInfo, UserId}

trait PointsOps[F[_]] {

  def loadPointsInfo(id: UserId): F[Option[PointsInfo]]

  def loadExpiringPoints(id: UserId): F[List[ExpiringPoints]]
}
