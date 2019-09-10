package com.rakuten.market.points.api.impl

import java.time.Instant

import cats.syntax.either._
import cats.syntax.flatMap._
import com.rakuten.market.points.api.core.{ServiceError, ServiceResult, PointsApiService => CoreApiService}
import com.rakuten.market.points.data._
import com.rakuten.market.points.storage.core.{PointsStorage, Transactional}
import com.rakuten.market.points.util.IdUtils._
import com.rakuten.market.points.util.TimeUtils._
import monix.eval.Task

private[api] class PointsApiService(pointsStorage: PointsStorage[Task])
                                   (implicit T: Transactional[Task, Task]) extends CoreApiService[Task] {

  def getPointsInfo(userId: UserId): Task[PointsInfo] =
    pointsStorage
      .getPointsInfo(userId)
      .map(_.getOrElse(PointsInfo.empty(userId)))

  def getExpiringPointsInfo(userId: UserId): Task[List[Points.Expiring]] = ???

  def getTransactionHistory(userId: UserId, from: Instant, to: Instant): Task[List[PointsTransaction.Confirmed]] =
    pointsStorage.getTransactionHistory(userId, from, to)

  def changePoints(amount: Points.Amount)(userId: UserId): Task[ServiceResult[Unit]] =
    T.transact {
      for {
        pointsInfo <- ensurePointsRecordExists(userId)
        time <- serverTime[Task]
        id <- generateTransactionId[Task]
        transaction = PointsTransaction.Confirmed(id, userId, time, amount, None, pointsInfo.total, None)
        _ <- pointsStorage.saveTransaction(transaction)
      } yield Right(())
    }.onErrorHandle(e => Either.left(ServiceError(e)))

  def startPointsTransaction(amount: Points.Amount)(userId: UserId): Task[ServiceResult[PointsTransaction.Id]] =
    T.transact {
      for {
        pointsInfo <- ensurePointsRecordExists(userId)
        time <- serverTime[Task]
        id <- generateTransactionId[Task]
        transaction = PointsTransaction.Pending(id, userId, time, amount, None, pointsInfo.total, None)
        _ <- pointsStorage.saveTransaction(transaction)
      } yield Right(id)
    }.onErrorHandle(e => Either.left(ServiceError(e)))

  def confirmPointsTransaction(transactionId: PointsTransaction.Id): Task[ServiceResult[Unit]] =
    pointsStorage.confirmTransaction(transactionId)
      .map(Either.right[ServiceError, Unit](_))
      .onErrorHandle(e => Either.left(ServiceError(e)))

  private def ensurePointsRecordExists(userId: UserId): Task[PointsInfo] =
    pointsStorage.getPointsInfo(userId).flatMap {
      case Some(info) =>
        Task.pure(info)
      case None =>
        val info = PointsInfo.empty(userId)
        pointsStorage.savePointsInfo(info) >> Task.pure(info)
    }
}
