package com.rakuten.market.points.http.core

import java.time.Instant

import com.rakuten.market.points.auth.core.AuthService
import com.rakuten.market.points.data._

trait ApiService[F[_]] extends AuthService[F] {

  def getInfo(userId: UserId): F[Option[PointsInfo]]

  def getExpiringPointsInfo(userId: UserId): F[List[ExpiringPoints]]

  def getTransactionHistory(userId: UserId, from: Instant, to: Instant): F[List[PointsTransaction]]

  def initAdd(amount: Points.Amount)(userId: UserId): F[Either[Unit, PointsTransaction.Id]]

  def completeAdd(amount: Points.Amount)(userId: UserId): F[Either[Unit, Unit]]

  def initPay(amount: Points.Amount)(userId: UserId): F[Either[Unit, PointsTransaction.Id]]

  def completePay(transactionId: PointsTransaction.Id): F[Either[Unit, Unit]]
}
