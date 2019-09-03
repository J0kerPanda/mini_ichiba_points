package com.rakuten.market.points.http.impl

import java.time.Instant

import com.rakuten.market.points.auth.core.AuthService
import com.rakuten.market.points.auth.{AuthToken, ServiceToken}
import com.rakuten.market.points.data.Points.Amount
import com.rakuten.market.points.data.PointsTransaction.Id
import com.rakuten.market.points.data._
import com.rakuten.market.points.http.core.{ServiceError, PointsApiService => CoreApiService}
import com.rakuten.market.points.storage.core.{PointsStorage, Transactional}
import monix.eval.Task
import com.rakuten.market.points.util.TimeUtils._

private[http] class PointsApiService(authService: AuthService[Task],
                                     pointsStorage: PointsStorage[Task])
                                    (implicit T: Transactional[Task, Task])extends CoreApiService[Task] {

  def authUser(token: AuthToken): Task[Option[UserId]] =
    authService.authUser(token)

  def authorizeProvidingPoints(token: ServiceToken): Task[Boolean] =
    authService.authorizeProvidingPoints(token)

  def authorizePointsPayment(token: ServiceToken): Task[Boolean] =
    authService.authorizePointsPayment(token)

  def getPointsInfo(userId: UserId): Task[PointsInfo] =
    pointsStorage
      .getPointsInfo(userId)
      .map(_.getOrElse(PointsInfo.empty(userId))) //todo add record if doesn't exist?

  def getExpiringPointsInfo(userId: UserId): Task[List[Points.Expiring]] = ???

  def getTransactionHistory(userId: UserId, from: Instant, to: Instant): Task[List[PointsTransaction]] = ???

  def initProvidingPoints(amount: Amount)(userId: UserId): Task[Either[ServiceError, Id]] = ???

  def completeProvidingPoints(transationId: PointsTransaction.Id): Task[Either[ServiceError, Unit]] = ???

  def initPointsPayment(amount: Points.Amount)(userId: UserId): Task[Either[ServiceError, PointsTransaction.Id]] = {
    //todo validate data
    T.transact {
      for {
        pointsInfo <- getPointsInfo(userId)
        time <- serverTime[Task]
        transaction = PointsTransaction.Unidentified(userId, time, Points.Simple(amount), pointsInfo.total, None)
        id <- pointsStorage.saveTransaction(transaction)
      } yield Right(id)
    }
  }

  def completePointsPayment(transactionId: PointsTransaction.Id): Task[Either[ServiceError, Unit]] =
    pointsStorage.setTransactionConfirmed(transactionId).map(Right(_))
}
