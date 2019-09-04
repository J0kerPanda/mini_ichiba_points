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

  override def savePointsInfo(info: PointsInfo): Task[Unit] =
    ctx.run {
      points.insert(lift(info))
    }.void

  override def getPointsInfo(userId: UserId): Task[Option[PointsInfo]] =
    ctx.run {
      points.filter(_.userId == lift(userId))
    }.map(_.headOption)

  override def getTransactionHistory(userId: UserId, from: Instant, to: Instant): Task[List[PointsTransaction.Confirmed]] = ???

  override def saveTransaction(transaction: PointsTransaction.Pending): Task[Unit] =
    ctx.run {
      pendingTransacton.insert(lift(transaction))
    }.void

  override def saveTransaction(transaction: PointsTransaction.Confirmed): Task[Unit] =
    ctx.transaction {
      for {
        _ <- ctx.run {
          confirmedTransaction.insert(lift(transaction))
        }.void
        _ <- ctx.run {
          points.filter(_.userId == lift(transaction.userId)).update(p => p.total -> (p.total + lift(transaction.amount)))
        }
      } yield ()
    }

  override def confirmTransaction(id: PointsTransaction.Id): Task[Unit] =
    ctx.transaction {
      for {
        i1 <- ctx.run {
          points.join(pendingTransacton.filter(_.id == lift(id))).on(_.userId == _.userId)
        }
        _ <- ctx.run {
          liftQuery(i1).foreach { case (_, t) =>
            confirmedTransaction.insert(
              _.id -> t.id, _.userId -> t.userId, _.time -> t.time, _.amount -> t.amount,
              _.expires -> t.expires, _.total -> t.total, _.comment -> t.comment
            )
          }
        }
        _ <- ctx.run {
          query[PointsTransaction.Pending].filter(_.id == lift(id)).delete
        }
        _ <- ctx.run {
          liftQuery(i1).foreach { case (info, trans) => points.filter(_.userId == info.userId).update(_.total -> (info.total + trans.amount)) }
        }
      } yield ()
    }

  override def removePendingTransaction(id: PointsTransaction.Id): Task[Unit] = ???
}
