package com.rakuten.market.points.service.core

import java.time.Instant

import com.rakuten.market.points.data._
import com.rakuten.market.points.service.impl.{PointsService => DefaultApiService}
import com.rakuten.market.points.settings.PointsTransactionSettings
import com.rakuten.market.points.storage.core.{PointsStorage, Transactional}
import monix.eval.Task

object PointsService {

  def default(settings: PointsTransactionSettings, storage: PointsStorage[Task])
             (implicit tr: Transactional[Task, Task]): PointsService[Task] =
    new DefaultApiService(settings, storage)
}

trait PointsService[F[_]] {

  def getPointsInfo(userId: UserId): F[PointsInfo]

  def getExpiringPointsInfo(userId: UserId): F[List[Points.Expiring]]

  def getTransactionHistory(from: Instant, to: Instant)(userId: UserId): F[List[PointsTransaction.Confirmed]]

  def changePoints(amount: Points.Amount,
                   expires: Option[Instant],
                   comment: Option[String])
                  (userId: UserId): F[ServiceResult[Unit]]

  def startPointsTransaction(amount: Points.Amount,
                             expires: Option[Instant],
                             comment: Option[String])
                            (userId: UserId): F[ServiceResult[PointsTransaction.Id]]

  def cancelPointsTransaction(transactionId: PointsTransaction.Id)(userId: UserId): F[ServiceResult[Unit]]

  def confirmPointsTransaction(transactionId: PointsTransaction.Id)(userId: UserId): F[ServiceResult[Unit]]

  def removeExpiredPoints: F[Unit]

  def removeExpiredTransactions: F[Unit]
}
