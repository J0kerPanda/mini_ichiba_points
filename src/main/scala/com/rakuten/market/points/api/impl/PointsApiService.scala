package com.rakuten.market.points.api.impl

import java.time.Instant

import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.rakuten.market.points.api.core.{EntityNotFound, InvalidRequest, ServiceError, ServiceResult, UnknownServiceError, PointsApiService => CoreApiService}
import com.rakuten.market.points.data.PointsTransaction.Id
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

  def getTransactionHistory(from: Instant, to: Instant)(userId: UserId): Task[List[PointsTransaction.Confirmed]] =
    pointsStorage.getTransactionHistory(userId, from, to)

  //todo check amount != 0 -> decoders for api or dependent types/???
  def changePoints(amount: Points.Amount, expires: Option[Instant])(userId: UserId): Task[ServiceResult[Unit]] = {
    import cats.instances.either._
    T.transact {
      for {
        pointsInfo <- ensurePointsRecordExists(userId)
        time <- serverTime[Task]
        id <- generateTransactionId[Task]
        transaction = PointsTransaction.Confirmed(id, userId, time, amount, expires, pointsInfo.total, None)
        res <- saveConsistently(pointsInfo, transaction)
      } yield res.void
    }.onErrorHandle(e => Either.left(UnknownServiceError(e)))
  }

  def startPointsTransaction(amount: Points.Amount, expires: Option[Instant])(userId: UserId): Task[ServiceResult[PointsTransaction.Id]] =
    T.transact {
      for {
        pointsInfo <- ensurePointsRecordExists(userId)
        time <- serverTime[Task]
        id <- generateTransactionId[Task]
        transaction = PointsTransaction.Pending(id, userId, time, amount, expires, pointsInfo.total, None)
        res <- saveConsistently(pointsInfo, transaction)
      } yield res
    }.onErrorHandle(e => Either.left(UnknownServiceError(e)))

  override def cancelPointsTransaction(transactionId: Id)(userId: UserId): Task[ServiceResult[Unit]] =
    pointsStorage
      .removePendingTransaction(userId, transactionId)
      .map { removed =>
        if (removed)
          Either.right[ServiceError, Unit](())
        else
          Either.left[ServiceError, Unit](EntityNotFound)
      }
      .onErrorHandle(e => Either.left(UnknownServiceError(e)))

  def confirmPointsTransaction(transactionId: PointsTransaction.Id)(userId: UserId): Task[ServiceResult[Unit]] =
    pointsStorage
      .confirmTransaction(userId, transactionId)
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

  private def saveConsistently(pointsInfo: PointsInfo,
                               transaction: PointsTransaction): Task[ServiceResult[PointsTransaction.Id]] = {
    val check = if (transaction.amount > 0)
      Task.pure(true)
    else
      pointsStorage
        .getTotalPendingDeduction(transaction.userId)
        .map(pendingDeduction =>(pointsInfo.total - pendingDeduction + transaction.amount) >= 0)

    check.ifM(
      ifTrue = transaction match {
        case t: PointsTransaction.Confirmed =>
          pointsStorage.saveTransaction(t).as(Right(t.id))
        case t: PointsTransaction.Pending =>
          pointsStorage.saveTransaction(t).as(Right(t.id))
      },
      ifFalse = Task.pure(Left(InvalidRequest))
    )
  }
}
