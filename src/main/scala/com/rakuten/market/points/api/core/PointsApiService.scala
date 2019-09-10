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

  def getTransactionHistory(from: Instant, to: Instant)(userId: UserId): F[List[PointsTransaction.Confirmed]]

  def changePoints(amount: Points.Amount, expires: Option[Instant])(userId: UserId): F[ServiceResult[Unit]]

  def startPointsTransaction(amount: Points.Amount, expires: Option[Instant])(userId: UserId): F[ServiceResult[PointsTransaction.Id]]

  def cancelPointsTransaction(transactionId: PointsTransaction.Id)(userId: UserId): F[ServiceResult[Unit]]

  def confirmPointsTransaction(transactionId: PointsTransaction.Id)(userId: UserId): F[ServiceResult[Unit]]
}
