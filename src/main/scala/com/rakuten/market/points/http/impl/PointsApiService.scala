package com.rakuten.market.points.http.impl

import java.time.Instant
import java.util.UUID

import com.rakuten.market.points.auth.{AuthToken, ServiceToken}
import com.rakuten.market.points.data.Points.Amount
import com.rakuten.market.points.data.{ExpiringPoints, Points, PointsInfo, PointsTransaction, UserId}
import com.rakuten.market.points.data.PointsTransaction.Id
import com.rakuten.market.points.http.core.{ServiceError, PointsApiService => CoreApiService}
import com.rakuten.market.points.storage.core.PointsStorage
import monix.eval.Task

private[http] class PointsApiService(pointsStorage: PointsStorage[Task]) extends CoreApiService[Task] {

  def authUser(token: AuthToken): Task[Option[UserId]] =
    Task.pure(Some(UUID.fromString("b982ba4a-ce23-11e9-a32f-2a2ae2dbcce4")))

  def authorizePointsAddition(token: ServiceToken): Task[Boolean] =
    Task.pure(true)

  def authorizePointsPayment(token: ServiceToken): Task[Boolean] =
    Task.pure(true)

  def getPointsInfo(userId: UserId): Task[Option[PointsInfo]] =
    pointsStorage.getPointsInfo(userId)

  def getExpiringPointsInfo(userId: UserId): Task[List[ExpiringPoints]] = ???

  def getTransactionHistory(userId: UserId, from: Instant, to: Instant): Task[List[PointsTransaction]] = ???

  def initProvidePoints(amount: Amount)(userId: UserId): Task[Either[ServiceError, Id]] = ???

  def completeProvidePoints(transationId: PointsTransaction.Id): Task[Either[ServiceError, Unit]] = ???

  def initPayment(amount: Points.Amount)(userId: UserId): Task[Either[ServiceError, PointsTransaction.Id]] = ???

  def completePayment(transactionId: PointsTransaction.Id): Task[Either[ServiceError, Unit]] = ???
}
