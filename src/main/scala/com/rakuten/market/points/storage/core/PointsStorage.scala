package com.rakuten.market.points.storage.core

import java.time.Instant

import com.rakuten.market.points.data.{PointsInfo, PointsTransaction, UserId}
import com.rakuten.market.points.storage.impl.{PostgresContext, PointsStorage => QuillStorage}
import monix.eval.Task

object PointsStorage {

  def postgres(implicit ctx: PostgresContext): PointsStorage[Task] =
    new QuillStorage()
}

trait PointsStorage[DBIO[_]] {

  def savePointsInfo(info: PointsInfo): DBIO[Unit]

  def getPointsInfo(userId: UserId): DBIO[Option[PointsInfo]]

  def getTransactionHistory(userId: UserId, from: Instant, to: Instant): DBIO[List[PointsTransaction.Confirmed]]

  def saveTransaction(transaction: PointsTransaction.Pending): DBIO[Unit]

  def saveTransaction(transaction: PointsTransaction.Confirmed): DBIO[Unit]

  def confirmTransaction(id: PointsTransaction.Id): DBIO[Boolean]

  def removePendingTransactions(from: Instant): DBIO[Unit]
}