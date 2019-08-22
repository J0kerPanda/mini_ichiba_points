package com.rakuten.market.points

import java.time.Instant
import java.util.UUID

import com.rakuten.market.points.auth.{AuthToken, ServiceToken}
import com.rakuten.market.points.data.Points.Amount
import com.rakuten.market.points.data.PointsTransaction.Id
import com.rakuten.market.points.data.{ExpiringPoints, Points, PointsInfo, PointsTransaction, UserId}
import com.rakuten.market.points.http.core.ApiService
import monix.eval.Task

object Mocks {

  val service = new ApiService[Task] {
    def authUser(token: AuthToken): Task[Option[UserId]] =
      Task.pure(Some(UUID.randomUUID()))

    def authorizePointsAddition(token: ServiceToken): Task[Boolean] = ???

    def authorizePointsPayment(token: ServiceToken): Task[Boolean] = ???

    def getInfo(userId: UserId): Task[Option[PointsInfo]] =
      Task.pure(None)

    def getExpiringPointsInfo(userId: UserId): Task[List[ExpiringPoints]] = ???

    def getTransactionHistory(userId: UserId, from: Instant, to: Instant): Task[List[PointsTransaction]] = ???

    override def initAdd(amount: Amount)(userId: UserId): Task[Either[Unit, Id]] = ???

    override def completeAdd(amount: Amount)(userId: UserId): Task[Either[Unit, Unit]] = ???

    def initPay(amount: Points.Amount)(userId: UserId): Task[Either[Unit, PointsTransaction.Id]] = ???

    def completePay(transactionId: PointsTransaction.Id): Task[Either[Unit, Unit]] = ???
  }
}
