package com.rakuten.market.points.algebra.core

import com.rakuten.market.points.data.{ExpiringPoints, PointsInfo, UserId}

trait PointsOps[F[_]] {

  def getPointsInfo(id: UserId): F[Option[PointsInfo]]

  def getExpiringPointsInfo(id: UserId): F[List[ExpiringPoints]]
}
