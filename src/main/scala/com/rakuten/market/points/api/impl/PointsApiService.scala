package com.rakuten.market.points.api.impl

import java.time.Instant

import cats.syntax.either._
import cats.syntax.flatMap._
import com.rakuten.market.points.api.core.{ConsistencyError, EntityNotFound, ServiceError, ServiceResult, UnknownServiceError, PointsApiService => CoreApiService}
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

  def getExpiringPointsInfo(userId: UserId): Task[List[Points.Expiring]] =
    for {
      points <- pointsStorage.getCurrentExpiringPoints(userId)
    } yield points.map(p => Points.Expiring(p.amount, p.expires))

  def getTransactionHistory(userId: UserId, from: Instant, to: Instant): Task[List[PointsTransaction.Confirmed]] =
    pointsStorage.getTransactionHistory(userId, from, to)

  def changePoints(amount: Points.Amount, expires: Option[Instant])(userId: UserId): Task[ServiceResult[Unit]] =
    //todo check points info
    T.transact {
      for {
        pointsInfo <- ensurePointsRecordExists(userId)
        time <- serverTime[Task]
        id <- generateTransactionId[Task]
        transaction = PointsTransaction.Confirmed(id, userId, time, amount, expires, pointsInfo.total, None)
        _ <- pointsStorage.saveTransaction(transaction)
      } yield Right(())
    }.onErrorHandle(e => Either.left(UnknownServiceError(e)))

  def startPointsTransaction(amount: Points.Amount, expires: Option[Instant])(userId: UserId): Task[ServiceResult[PointsTransaction.Id]] =
    T.transact {
      //todo check points info + pendingTransactionsTotal
      for {
        pointsInfo <- ensurePointsRecordExists(userId)
        pendingDeduction <- pointsStorage.getTotalPendingDeduction(userId)
        res <- if (pointsInfo.total - pendingDeduction >= 0)
          for {
            time <- serverTime[Task]
            id <- generateTransactionId[Task]
            transaction = PointsTransaction.Pending(id, userId, time, amount, expires, pointsInfo.total, None)
            _ <- pointsStorage.saveTransaction(transaction)
          } yield Right(id)
        else Task.pure(Left(ConsistencyError))
      } yield res
    }.onErrorHandle(e => Either.left(UnknownServiceError(e)))

  def confirmPointsTransaction(transactionId: PointsTransaction.Id): Task[ServiceResult[Unit]] =
    pointsStorage
      .confirmTransaction(transactionId)
      .map { confirmed =>
        if (confirmed)
          Either.right[ServiceError, Unit](())
        else
          Either.left[ServiceError, Unit](EntityNotFound)
      }
      .onErrorHandle(e => Either.left(UnknownServiceError(e)))

  private def ensurePointsRecordExists(userId: UserId): Task[PointsInfo] =
    pointsStorage
      .getPointsInfo(userId)
      .flatMap {
        case Some(info) =>
          Task.pure(info)
        case None =>
          val info = PointsInfo.empty(userId)
          pointsStorage.savePointsInfo(info) >> Task.pure(info)
      }
}
