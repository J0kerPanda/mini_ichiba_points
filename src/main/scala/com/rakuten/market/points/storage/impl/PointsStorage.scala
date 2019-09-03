package com.rakuten.market.points.storage.impl

import java.time.Instant

import com.rakuten.market.points.data.{PointsInfo, PointsTransaction, UserId}
import com.rakuten.market.points.storage.core.{PointsStorage => CorePointsStorage}
import com.rakuten.market.points.storage.util.PostgresContext
import monix.eval.Task

private[storage] class PointsStorage(implicit ctx: PostgresContext) extends CorePointsStorage[Task] {
  import Schema._
  import ctx._

  override def getPointsInfo(userId: UserId): Task[Option[PointsInfo]] =
    ctx.run {
      points.filter(_.userId == lift(userId))
    }.map(_.headOption)

  override def getTransactionHistory(userId: UserId, from: Instant, to: Instant): Task[List[PointsTransaction.Confirmed]] = ???

  override def saveTransaction(transaction: PointsTransaction.Unidentified): Task[PointsTransaction.Id] = ???

  override def setTransactionConfirmed(id: PointsTransaction.Id): Task[Unit] = ???

  override def setTransactionCancelled(id: PointsTransaction.Id): Task[Unit] = ???
}
