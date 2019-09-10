package com.rakuten.market.points.storage.impl

import java.time.Instant

import cats.syntax.functor._
import com.rakuten.market.points.data
import com.rakuten.market.points.data.Points.Amount
import com.rakuten.market.points.data.{Points, PointsInfo, PointsTransaction, UserId}
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

  override def getCurrentExpiringPoints(userId: UserId): Task[List[Points.Expiring]] =
    ctx.run {
      expiringPoints.filter(_.userId == lift(userId))
    }.map(_.map(p => Points.Expiring(p.amount, p.expires)))

  override def getPendingTransactionsTotalPayment(userId: UserId): Task[Points.Amount] =
    ctx.run {
      pendingTransacton.map(_.amount).filter(_ < 0).sum
    }.map(_.getOrElse(0))


  override def getTransactionHistory(userId: UserId,
                                     from: Instant,
                                     to: Instant): Task[List[PointsTransaction.Confirmed]] =
    ctx.run {
      confirmedTransaction.filter { t =>
        t.time >= lift(from) && t.time <= lift(to) && t.userId == lift(userId)
      }
    }

  override def saveTransaction(transaction: PointsTransaction.Pending): Task[Unit] =
    ctx.transaction {
      for {
        res <- ctx.run {
          pendingTransacton.insert(lift(transaction))
        }.void
        _ <- transaction match {
          case PointsTransaction.Pending(id, userId, _, amount, Some(expires), _, _) =>
            ctx.run {
              query[ExpiringPoints].insert(lift(ExpiringPoints(id, userId, amount, expires)))
            }
          case _ =>
            Task.unit
        }
      } yield res
    }


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
          liftQuery(i1.map(_._2)).foreach { t =>
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
          liftQuery(i1.map(_._2).filter(_.expires.nonEmpty)).foreach { t =>
            expiringPoints.insert(ExpiringPoints(t.id, t.userId, t.amount, t.expires.orNull))
          }
        }
        _ <- ctx.run {
          liftQuery(i1).foreach { case (info, trans) =>
            points.filter(_.userId == info.userId).update(i => i.total -> (i.total + trans.amount))
          }
        }
      } yield i1.nonEmpty
    }

  override def removePendingTransactions(from: Instant): Task[Unit] =
    ctx.run {
      confirmedTransaction.filter(_.time <= lift(from)).delete
    }.void
}
