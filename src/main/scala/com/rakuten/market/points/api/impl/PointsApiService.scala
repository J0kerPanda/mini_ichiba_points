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
  def changePoints(amount: Points.Amount, expires: Option[Instant])(userId: UserId): Task[ServiceResult[Unit]] =
    T.transact {
      createConfirmedTransaction(userId, expires, amount).flatMap((saveConsistently _).tupled)
    }.onErrorHandle(wrapUnknownError[PointsTransaction.Id]).map(_.map(_ => ()))

  def startPointsTransaction(amount: Points.Amount, expires: Option[Instant])(userId: UserId): Task[ServiceResult[PointsTransaction.Id]] =
    T.transact {
      createPendingTransaction(userId, expires, amount).flatMap((saveConsistently _).tupled)
    } .onErrorHandle(wrapUnknownError[PointsTransaction.Id])

  override def cancelPointsTransaction(transactionId: Id)(userId: UserId): Task[ServiceResult[Unit]] =
    pointsStorage
      .removePendingTransaction(userId, transactionId)
      .map(wrapNotFoundError)
      .onErrorHandle(wrapUnknownError)

  def confirmPointsTransaction(transactionId: PointsTransaction.Id)(userId: UserId): Task[ServiceResult[Unit]] =
    pointsStorage
      .confirmTransaction(userId, transactionId)
      .map(wrapNotFoundError)
      .onErrorHandle(wrapUnknownError)

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

  //todo validate transaction -> expires -> >0, != 0, etc
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

  private def wrapNotFoundError(found: Boolean): ServiceResult[Unit] =
    if (found)
      Either.right[ServiceError, Unit](())
    else
      Either.left[ServiceError, Unit](EntityNotFound)

  private def wrapUnknownError[A](e: Throwable): ServiceResult[A] =
    Either.left(UnknownServiceError(e))

  private def createConfirmedTransaction(userId: UserId,
                                         expires: Option[Instant],
                                         amount: Points.Amount): Task[(PointsInfo, PointsTransaction.Confirmed)] =
    for {
      pointsInfo <- ensurePointsRecordExists(userId)
      time <- serverTime[Task]
      id <- generateTransactionId[Task]
    } yield (pointsInfo, PointsTransaction.Confirmed(id, userId, time, amount, expires, pointsInfo.total, None))

  private def createPendingTransaction(userId: UserId,
                                       expires: Option[Instant],
                                       amount: Points.Amount): Task[(PointsInfo, PointsTransaction.Pending)] =
    for {
      pointsInfo <- ensurePointsRecordExists(userId)
      time <- serverTime[Task]
      id <- generateTransactionId[Task]
    } yield (pointsInfo, PointsTransaction.Pending(id, userId, time, amount, expires, pointsInfo.total, None))
}
