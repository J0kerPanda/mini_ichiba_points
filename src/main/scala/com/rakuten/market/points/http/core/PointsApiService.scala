package com.rakuten.market.points.http.core

import java.time.Instant

import com.rakuten.market.points.auth.core.AuthService
import com.rakuten.market.points.data._
import com.rakuten.market.points.storage.core.{PointsStorage, Transactional}
import monix.eval.Task
import com.rakuten.market.points.http.impl.{PointsApiService => DefaultApiService}

object PointsApiService {

  def default(authService: AuthService[Task],
              pointsStorage: PointsStorage[Task])
             (implicit tr: Transactional[Task, Task]): PointsApiService[Task] =
    new DefaultApiService(authService, pointsStorage)
}

trait PointsApiService[F[_]] extends AuthService[F] {

  def getPointsInfo(userId: UserId): F[PointsInfo]

  def getExpiringPointsInfo(userId: UserId): F[List[Points.Expiring]]

  def getTransactionHistory(userId: UserId, from: Instant, to: Instant): F[List[PointsTransaction]]

  def initProvidingPoints(amount: Points.Amount)(userId: UserId): F[Either[ServiceError, PointsTransaction.Id]]

  def initPointsPayment(amount: Points.Amount)(userId: UserId): F[Either[ServiceError, PointsTransaction.Id]]

  def completeTransaction(transactionId: PointsTransaction.Id): F[Either[ServiceError, Unit]]
}
