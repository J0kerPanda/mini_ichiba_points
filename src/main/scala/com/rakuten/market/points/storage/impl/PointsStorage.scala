package com.rakuten.market.points.storage.impl

import java.time.Instant

import cats.syntax.functor._
import com.rakuten.market.points.data.{PointsInfo, PointsTransaction, UserId}
import com.rakuten.market.points.storage.core.{PointsStorage => CorePointsStorage}
import monix.eval.Task

private[storage] class PointsStorage(protected implicit val ctx: PostgresContext)
  extends CorePointsStorage[Task] with Quotes {

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

  override def getCurrentExpiringTransactions(userId: UserId): Task[List[PointsTransaction.Confirmed]] =
    ctx.run {
      confirmedTransaction.filter(t => t.userId == lift(userId) && t.expires.nonEmpty)
    }

  override def getTransactionHistory(userId: UserId,
                                     from: Instant,
                                     to: Instant): Task[List[PointsTransaction.Confirmed]] =
    ctx.run {
      confirmedTransaction.filter { t =>
        t.time >= lift(from) && t.time <= lift(to) && t.userId == lift(userId)
      }
    }

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

  override def confirmTransaction(id: PointsTransaction.Id): Task[Boolean] =
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
          pendingTransacton.filter(_.id == lift(id)).delete
        }
        _ <- ctx.run {
          liftQuery(i1).foreach { case (info, trans) => points.filter(_.userId == info.userId).update(i => i.total -> (i.total + trans.amount)) }
        }
      } yield i1.nonEmpty
    }

  override def removePendingTransactions(from: Instant): Task[Unit] =
    ctx.run {
      confirmedTransaction.filter(_.time <= lift(from)).delete
    }.void
}
