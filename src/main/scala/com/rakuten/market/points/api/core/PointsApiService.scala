package com.rakuten.market.points.api.core

import java.time.Instant

import com.rakuten.market.points.api.impl.{PointsApiService => DefaultApiService}
import com.rakuten.market.points.data._
import com.rakuten.market.points.storage.core.{PointsStorage, Transactional}
import monix.eval.Task

object PointsApiService {

  def default(pointsStorage: PointsStorage[Task])
             (implicit tr: Transactional[Task, Task]): PointsApiService[Task] =
    new DefaultApiService(pointsStorage)
}

trait PointsApiService[F[_]] {

  def getPointsInfo(userId: UserId): F[PointsInfo]

  def getExpiringPointsInfo(userId: UserId): F[List[Points.Expiring]]

  def getTransactionHistory(userId: UserId, from: Instant, to: Instant): F[List[PointsTransaction]]

  def changePoints(amount: Points.Amount)(userId: UserId): F[Either[ServiceError, Unit]]

  def startPointsTransaction(amount: Points.Amount)(userId: UserId): F[Either[ServiceError, PointsTransaction.Id]]

  def confirmPointsTransaction(transactionId: PointsTransaction.Id): F[Either[ServiceError, Unit]]
}
