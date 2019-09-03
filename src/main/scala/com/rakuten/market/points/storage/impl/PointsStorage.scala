package com.rakuten.market.points.storage.impl

import java.time.Instant

import cats.syntax.functor._
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

  override def saveTransaction(transaction: PointsTransaction.Unconfirmed): Task[Unit] =
    ctx.run {
      unconfirmedTransaction.insert(lift(transaction))
    }.void

  override def setTransactionConfirmed(id: PointsTransaction.Id): Task[Unit] =
    ctx.transaction {
      for {
        i1 <- ctx.run {
          points.join(unconfirmedTransaction.filter(_.id == lift(id))).on(_.userId == _.userId)
        }
        _ <- ctx.run {
          liftQuery(i1).foreach { case (info, trans) => points.filter(_.userId == info.userId).update(p => (p.total, p.total + trans.amount)) }
        }
      } yield ()
    }

  override def setTransactionCancelled(id: PointsTransaction.Id): Task[Unit] = ???
}
