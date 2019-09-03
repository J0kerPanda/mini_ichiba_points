package com.rakuten.market.points.algebra.core

import com.rakuten.market.points.data.{ExpiringPoints, PointsInfo, UserId}
import com.rakuten.market.points.algebra.impl.{PointsOps => TaskOps}
import com.rakuten.market.points.storage.core.PointsStorage
import monix.eval.Task

object PointsOps {

  def task(pointsStorage: PointsStorage[Task]): PointsOps[Task] =
    new TaskOps(pointsStorage)
}

trait PointsOps[F[_]] {

  def getPointsInfo(id: UserId): F[Option[PointsInfo]]

  def getExpiringPointsInfo(id: UserId): F[List[ExpiringPoints]]
}
