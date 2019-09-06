package com.rakuten.market.points.api.impl

import java.time.Instant

import cats.syntax.flatMap._
import com.rakuten.market.points.api.core.{ServiceError, PointsApiService => CoreApiService}
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
      .map(_.getOrElse(PointsInfo.empty(userId))) //todo add record if doesn't exist?

  def getExpiringPointsInfo(userId: UserId): Task[List[Points.Expiring]] = ???

  def getTransactionHistory(userId: UserId, from: Instant, to: Instant): Task[List[PointsTransaction]] = ???

  def changePoints(amount: Points.Amount)(userId: UserId): Task[Either[ServiceError, Unit]] =
    //todo check authority? -> serviceToken
    T.transact {
      for {
        pointsInfo <- ensurePointsRecordExists(userId)
        time <- serverTime[Task]
        id <- generateTransactionId
        transaction = PointsTransaction.Confirmed(id, userId, time, amount, None, pointsInfo.total, None)
        _ <- pointsStorage.saveTransaction(transaction)
      } yield Right(())
    }

  def startPointsTransaction(amount: Points.Amount)(userId: UserId): Task[Either[ServiceError, PointsTransaction.Id]] = {
    //todo validate data
    //todo check authority? -> serviceToken
    T.transact {
      for {
        pointsInfo <- ensurePointsRecordExists(userId)
        time <- serverTime[Task]
        id <- generateTransactionId
        transaction = PointsTransaction.Pending(id, userId, time, amount, None, pointsInfo.total, None)
        _ <- pointsStorage.saveTransaction(transaction)
      } yield Right(id)
    }
  }

  //todo return error on false
  def confirmPointsTransaction(transactionId: PointsTransaction.Id): Task[Either[ServiceError, Unit]] =
    pointsStorage.confirmTransaction(transactionId).map(Right(_))

  private def ensurePointsRecordExists(userId: UserId): Task[PointsInfo] =
    pointsStorage.getPointsInfo(userId).flatMap {
      case Some(info) =>
        Task.pure(info)
      case None =>
        val info = PointsInfo.empty(userId)
        pointsStorage.savePointsInfo(info) >> Task.pure(info)
    }
}
