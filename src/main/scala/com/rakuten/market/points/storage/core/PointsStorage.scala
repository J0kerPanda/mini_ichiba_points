package com.rakuten.market.points.storage.core

import java.time.Instant

import com.rakuten.market.points.data.{Points, PointsInfo, PointsTransaction, UserId}
import com.rakuten.market.points.storage.impl.{PointsStorage => QuillStorage}
import com.rakuten.market.points.storage.util.PostgresContext
import monix.eval.Task

object PointsStorage {

  def postgres(implicit ctx: PostgresContext): PointsStorage[Task] =
    new QuillStorage()
}

trait PointsStorage[DBIO[_]] {

  def getPointsInfo(userId: UserId): DBIO[Option[PointsInfo]]

  def getTransactionHistory(userId: UserId, from: Instant, to: Instant): DBIO[List[PointsTransaction.Confirmed]]

  def saveTransaction(transaction: PointsTransaction.Unconfirmed): DBIO[Unit]

  def setTransactionConfirmed(id: PointsTransaction.Id): DBIO[Unit]

  def setTransactionCancelled(id: PointsTransaction.Id): DBIO[Unit]
}