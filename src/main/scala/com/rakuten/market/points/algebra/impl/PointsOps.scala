package com.rakuten.market.points.algebra.impl

import com.rakuten.market.points.algebra.core.{PointsOps => CorePointsOps}
import com.rakuten.market.points.data.{ExpiringPoints, PointsInfo, UserId}
import com.rakuten.market.points.storage.core.PointsStorage
import monix.eval.Task

private[algebra] class PointsOps(storage: PointsStorage[Task]) extends CorePointsOps[Task] {

  override def getPointsInfo(id: UserId): Task[Option[PointsInfo]] =
    storage.getPointsInfo(id)

  override def getExpiringPointsInfo(id: UserId): Task[List[ExpiringPoints]] =
    ???
}