package com.rakuten.market.points.app

import cats.syntax.flatMap._
import cats.syntax.traverse._
import com.rakuten.market.points.data.PointsTransaction
import com.rakuten.market.points.settings.{ExpiringPointsSettings, PointsTransactionSettings}
import com.rakuten.market.points.storage.core.{ExpiringPoints, PointsStorage, Transactional}
import com.rakuten.market.points.util.{IdUtils, TimeUtils}
import monix.eval.Task

import scala.concurrent.duration.FiniteDuration

private[app] object Schedule {

  implicit class ScheduleOps[A](val job: Task[A]) extends AnyVal {
    def runEvery(interval: FiniteDuration): Task[A] =
      job >> Task.sleep(interval) >> runEvery(interval)
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

  def removeExpiredPoints(settings: ExpiringPointsSettings, storage: PointsStorage[Task])
                         (implicit T: Transactional[Task, Task]): Task[Unit] = {
    val job = for {
      time <- TimeUtils.serverTime[Task]
      res <- storage.getCurrentExpiringPoints(time)
        .mapEvalF(removeExpiringPoints(storage))
        .completedL
    } yield res

    job.runEvery(settings.checkEvery)
  }

  private def removeExpiringPoints(storage: PointsStorage[Task])(expired: ExpiringPoints)
                                  (implicit T: Transactional[Task, Task]): Task[Unit] = {
    import cats.instances.option._
    T.transact {
      for {
        time <- TimeUtils.serverTime[Task]
        id <- IdUtils.generateTransactionId[Task]
        info <- storage.getPointsInfo(expired.userId)
        _ <- info.traverse { i =>
          storage.saveTransaction(
            PointsTransaction.Confirmed(id, expired.userId, time, -expired.amount, None, i.total - expired.amount, None)
          )
        }
      } yield ()
    }
  }
}
