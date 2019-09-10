package com.rakuten.market.points.storage.core

import java.time.Instant

import com.rakuten.market.points.data.{Points, PointsInfo, PointsTransaction, UserId}
import com.rakuten.market.points.storage.impl.{PostgresContext, PointsStorage => QuillStorage}
import monix.eval.Task
import monix.reactive.Observable

object PointsStorage {

  def postgres(implicit ctx: PostgresContext): PointsStorage[Task] =
    new QuillStorage()
}

trait PointsStorage[DBIO[_]] {

  def savePointsInfo(info: PointsInfo): DBIO[Unit]

  def getPointsInfo(userId: UserId): DBIO[Option[PointsInfo]]

  def getCurrentExpiringPoints(userId: UserId): DBIO[List[ExpiringPoints]]

  def getCurrentExpiringPoints(expireBefore: Instant): Observable[ExpiringPoints]

  /** @return (non-negative) amount of points to be deducted from user
    */
  def getTotalPendingDeduction(userId: UserId): DBIO[Points.Amount]

  def getTransactionHistory(userId: UserId, from: Instant, to: Instant): DBIO[List[PointsTransaction.Confirmed]]

  def saveTransaction(transaction: PointsTransaction.Pending): DBIO[Unit]

  def saveTransaction(transaction: PointsTransaction.Confirmed): DBIO[Unit]

  def confirmTransaction(userId: UserId, id: PointsTransaction.Id): DBIO[Boolean]

  def removePendingTransaction(userId: UserId, id: PointsTransaction.Id): DBIO[Boolean]

  def removePendingTransactions(from: Instant): DBIO[Unit]
}