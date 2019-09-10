package com.rakuten.market.points.app

import com.rakuten.market.points.settings.{ExpiringPointsSettings, PointsTransactionSettings}
import com.rakuten.market.points.storage.core.PointsStorage
import com.rakuten.market.points.util.{IdUtils, TimeUtils}
import monix.eval.Task
import cats.syntax.flatMap._
import com.rakuten.market.points.data.PointsTransaction

import scala.concurrent.duration.FiniteDuration

private[app] object Schedule {

  implicit class ScheduleOps[A](val job: Task[A]) extends AnyVal {
    def runEvery(interval: FiniteDuration): Task[A] =
      job >> Task.sleep(interval) >> job
  }

  def removePendingTransactions(settings: PointsTransactionSettings,
                                storage: PointsStorage[Task]): Task[Unit] = {
    val job = for {
      time <- TimeUtils.serverTime[Task]
      from = time.minusMillis(settings.expirationInterval.toMillis)
      res <- storage.removePendingTransactions(from)
    } yield res

    job.runEvery(settings.checkEvery)
  }

  def removeExpiredPoints(settings: ExpiringPointsSettings, storage: PointsStorage[Task]): Task[Unit] = {
    val job = for {
      time <- TimeUtils.serverTime[Task]
      res <- storage.getCurrentExpiringPoints(time)
        .mapEvalF { expired =>
          for {
            id <- IdUtils.generateTransactionId[Task]
            time <- TimeUtils.serverTime[Task]
            info <- storage.getPointsInfo(expired.userId)
            res <- info match {
              case Some(i) =>
                storage.saveTransaction(
                  PointsTransaction.Confirmed(id, expired.userId, time, -expired.amount, None, i.total - expired.amount, None)
                )
              case None =>
                Task.unit
            }
          } yield res
        }
        .completedL
    } yield res

    job.runEvery(settings.checkEvery)
  }
}
