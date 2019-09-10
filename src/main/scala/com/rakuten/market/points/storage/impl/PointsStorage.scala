package com.rakuten.market.points.storage.impl

import java.time.Instant

import cats.data.OptionT
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import com.rakuten.market.points.data.{Points, PointsInfo, PointsTransaction, UserId}
import com.rakuten.market.points.storage.core.{ExpiringPoints, PointsStorage => CorePointsStorage}
import monix.eval.Task
import monix.reactive.Observable

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

  override def getCurrentExpiringPoints(userId: UserId): Task[List[ExpiringPoints]] =
    ctx.run {
      expiringPoints.filter(_.userId == lift(userId))
    }

  override def getCurrentExpiringPoints(expireBefore: Instant): Observable[ExpiringPoints] =
    ctx.stream {
      expiringPoints.filter(_.expires <= lift(expireBefore))
    }

  override def getTotalPendingDeduction(userId: UserId): Task[Points.Amount] =
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
            //todo subtract from expiring points if negative
            Task.unit
        }
      } yield res
    }


  override def saveTransaction(transaction: PointsTransaction.Confirmed): Task[Unit] =
    ctx.transaction {
      for {
        // Save confirmed transaction
        _ <- ctx.run {
          confirmedTransaction.insert(lift(transaction))
        }.void
        // Update points info
        _ <- ctx.run {
          points.filter(_.userId == lift(transaction.userId)).update(p => p.total -> (p.total + lift(transaction.amount)))
        }
        // Add records to expiring points if required
        _ <- transaction.expires match {
          case Some(expires) =>
            ctx.run {
              expiringPoints.insert(lift(ExpiringPoints(transaction.id, transaction.userId, transaction.amount, expires)))
            }
          case None =>
            Task.unit
        }
        // Remove expiring points if required
        res <- if (transaction.amount < 0)
          subtractFromPendingPoints(transaction.userId, -transaction.amount)
        else
          Task.unit
      } yield res
    }

  override def confirmTransaction(id: PointsTransaction.Id): Task[Boolean] = {
    import cats.instances.list._
    ctx.transaction {
      for {
        // Find transaction and points info
        joined <- ctx.run {
          query[PointsInfo].join(query[PointsTransaction.Pending].filter(_.id == lift(id))).on(_.userId == _.userId)
        }
        transaction = joined.map(_._2)
        // Save confirmed transaction
        _ <- ctx.run {
          liftQuery(transaction).foreach { t =>
            confirmedTransaction.insert(
              _.id -> t.id, _.userId -> t.userId, _.time -> t.time, _.amount -> t.amount,
              _.expires -> t.expires, _.total -> t.total, _.comment -> t.comment
            )
          }
        }
        // Remove pending transaction
        _ <- ctx.run {
          pendingTransacton.filter(_.id == lift(id)).delete
        }
        // Add records to expiring points if required
        _ <- ctx.run {
          liftQuery(transaction.filter(_.expires.nonEmpty)).foreach { t =>
            expiringPoints.insert(ExpiringPoints(t.id, t.userId, t.amount, t.expires.orNull))
          }
        }
        // Remove expiring points if required
        _ <- transaction
          .filter(_.amount < 0)
          .traverse(t => subtractFromPendingPoints(t.userId, -t.amount))
        // Update points info
        _ <- ctx.run {
          liftQuery(joined).foreach { case (info, trans) =>
            points.filter(_.userId == info.userId).update(i => i.total -> (i.total + trans.amount))
          }
        }
      } yield joined.nonEmpty
    }
  }

  override def removePendingTransactions(from: Instant): Task[Unit] =
    ctx.run {
      confirmedTransaction.filter(_.time <= lift(from)).delete
    }.void

  //todo check that invalid situation (negative points) impossible
  private def subtractFromPendingPoints(userId: UserId, amount: Points.Amount): Task[Unit] =
    OptionT(
      ctx.run {
        query[ExpiringPoints].filter(_.userId == lift(userId)).sortBy(_.expires)(Ord.desc).take(1)
      }.map(_.headOption)
    ).semiflatMap { exp =>
      val newAmount = amount - exp.amount
      if (exp.amount <= amount) {
        ctx.run {
          query[ExpiringPoints].filter(_.transactionId == lift(exp.transactionId)).delete
        } >> subtractFromPendingPoints(userId, newAmount)
      } else {
        ctx.run {
          query[ExpiringPoints]
            .filter(_.transactionId == lift(exp.transactionId))
            .update(p => p.amount -> (p.amount - lift(amount)))
        }.void
      }
    }.value.void
}
