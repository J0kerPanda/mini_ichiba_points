package com.rakuten.market.points.http.core

import java.time.Instant

import com.rakuten.market.points.auth.core.AuthService
import com.rakuten.market.points.data._
import com.rakuten.market.points.storage.core.PointsStorage
import monix.eval.Task

import com.rakuten.market.points.http.impl.{PointsApiService => DefaultApiService}

object PointsApiService {

  def default(pointsStorage: PointsStorage[Task]): PointsApiService[Task] =
    new DefaultApiService(pointsStorage)
}

trait PointsApiService[F[_]] extends AuthService[F] {

  def getPointsInfo(userId: UserId): F[Option[PointsInfo]]

  def getExpiringPointsInfo(userId: UserId): F[List[ExpiringPoints]]

  def getTransactionHistory(userId: UserId, from: Instant, to: Instant): F[List[PointsTransaction]]

  def initProvidePoints(amount: Points.Amount)(userId: UserId): F[Either[ServiceError, PointsTransaction.Id]]

  def completeProvidePoints(transactionId: PointsTransaction.Id): F[Either[ServiceError, Unit]]

  def initPayment(amount: Points.Amount)(userId: UserId): F[Either[ServiceError, PointsTransaction.Id]]

  def completePayment(transactionId: PointsTransaction.Id): F[Either[ServiceError, Unit]]
}
