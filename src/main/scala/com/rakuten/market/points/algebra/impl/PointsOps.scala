package com.rakuten.market.points.algebra.impl

import com.rakuten.market.points.algebra.core.{PointsOps => CorePointsOps}
import com.rakuten.market.points.data.{ExpiringPoints, PointsInfo, UserId}
import monix.eval.Task

class PointsOps extends CorePointsOps[Task] {

  override def getPointsInfo(id: UserId): Task[Option[PointsInfo]] = ???

  override def getExpiringPointsInfo(id: UserId): Task[List[ExpiringPoints]] = ???
}
