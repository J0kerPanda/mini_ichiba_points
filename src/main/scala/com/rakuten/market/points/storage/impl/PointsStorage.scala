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
import cats.syntax.applicative._

private[storage] class PointsStorage(protected implicit val ctx: PostgresContext)
  extends CorePointsStorage[Task] with Quotes with Schema {

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

  override def getCurrentExpiringPoints(expireBefore: Instant): Observable[ExpiringPoints] = {
    println(expireBefore)
    ctx.stream {
      expiringPoints.filter(_.expires <= lift(expireBefore))
    }
  }

  override def getTotalPendingDeduction(userId: UserId): Task[Points.Amount] =
    ctx.run {
      pendingTransaction.map(_.amount).filter(_ < 0).sum
    }.map(_.getOrElse(0))

  override def getTransactionHistory(userId: UserId,
                                     from: Instant,
                                     to: Instant): Task[List[PointsTransaction.Confirmed]] =
    ctx.run {
      confirmedTransaction.filter { t =>
        t.time >= lift(from) && t.time <= lift(to) && t.userId == lift(userId)
      }.sortBy(_.time)(Ord.desc)
    }

  override def saveTransaction(transaction: PointsTransaction.Confirmed): Task[Unit] = {
    import cats.instances.option._
    ctx.transaction {
      for {
        // Save confirmed transaction
        _ <- ctx.run {
          confirmedTransaction.insert(lift(transaction))
        }
        // Add records to expiring points if required
        _ <- transaction.expires.traverse { expires =>
          ctx.run {
            expiringPoints.insert(lift(ExpiringPoints(transaction.id, transaction.userId, transaction.amount, expires)))
          }
        }
        // Remove expiring points if required
        _ <- subtractFromPendingPoints(transaction.userId, -transaction.amount).whenA(transaction.amount < 0)
        // Update points info
        res <- updatePointsInfo(transaction.userId, transaction.amount)
      } yield res
    }
  }

  override def saveTransaction(transaction: PointsTransaction.Pending): Task[Unit] =
    ctx.transaction {
      ctx.run {
        pendingTransaction.insert(lift(transaction))
      }.void
    }

  override def confirmTransaction(userId: UserId, id: PointsTransaction.Id): Task[Boolean] = {
    import cats.instances.list._
    ctx.transaction {
      for {
        // Find transaction and points info
        joined <- ctx.run {
          points
            .join(pendingTransaction.filter(t => t.id == lift(id) && t.userId == lift(userId)))
            .on(_.userId == _.userId)
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
          pendingTransaction.filter(_.id == lift(id)).delete
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
        res <- joined.traverse { case (_, trans) =>
          updatePointsInfo(trans.userId, trans.amount)
        }
      } yield res.nonEmpty
    }
  }

  override def removePendingTransaction(userId: UserId, id: PointsTransaction.Id): Task[Boolean] =
    ctx.run {
      pendingTransaction.filter(t => t.id == lift(id) && t.userId == lift(userId)).delete
    }.map(_ > 0)

  override def removePendingTransactions(from: Instant): Task[Unit] =
    ctx.run {
      pendingTransaction.filter(_.time <= lift(from)).delete
    }.void

  /** Change user points total by a given amount of points and refresh pending points information.
    */
  private def updatePointsInfo(userId: UserId, amount: Points.Amount): Task[Unit] =
    for {
      closestExpiring <- ctx.run {
        expiringPoints.filter(_.userId == lift(userId)).sortBy(_.expires)(Ord.asc)
      }.map(_.headOption.map(e => Points.Expiring(e.amount, e.expires)))
      totalExpiring <- ctx.run {
        expiringPoints.filter(_.userId == lift(userId)).map(_.amount).sum
      }
      _ <- ctx.run {
        points.filter(_.userId == lift(userId)).update(
          i => i.total -> (i.total + lift(amount)),
          i => i.closestExpiring.map(_.value) -> lift(closestExpiring.map(_.value)),
          i => i.closestExpiring.map(_.expires) -> lift(closestExpiring.map(_.expires)),
          i => i.totalExpiring -> lift(totalExpiring.getOrElse(0))
        )
      }
    } yield ()

  //todo check that invalid situation (negative points) impossible
  private def subtractFromPendingPoints(userId: UserId, amount: Points.Amount): Task[Unit] =
    OptionT(
      ctx.run {
        query[ExpiringPoints].filter(_.userId == lift(userId)).sortBy(_.expires)(Ord.desc).take(1)
      }.map(_.headOption)
    ).semiflatMap { exp =>
      val newAmount = amount - exp.amount
      if (exp.amount <= amount) {
        // Continue subtracting
        ctx.run {
          query[ExpiringPoints].filter(_.transactionId == lift(exp.transactionId)).delete
        } >> subtractFromPendingPoints(userId, newAmount).whenA(newAmount > 0)
      } else {
        //
        ctx.run {
          query[ExpiringPoints]
            .filter(_.transactionId == lift(exp.transactionId))
            .update(p => p.amount -> (p.amount - lift(amount)))
        }.void
      }
    }.value.void
}
