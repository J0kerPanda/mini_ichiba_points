package com.rakuten.market.points.app

import cats.syntax.flatMap._
import com.rakuten.market.points.service.core.PointsService
import com.rakuten.market.points.settings.{ExpiringPointsSettings, PointsTransactionSettings}
import monix.eval.Task

import scala.concurrent.duration.FiniteDuration

private[app] object Schedule {

  implicit class ScheduleOps[A](val job: Task[A]) extends AnyVal {
    def runEvery(interval: FiniteDuration): Task[A] =
      job >> Task.sleep(interval) >> runEvery(interval)
  }

  def removeExpiredTransactions(settings: PointsTransactionSettings,
                                service: PointsService[Task]): Task[Unit] =
    service.removeExpiredTransactions.runEvery(settings.checkEvery)

  def removeExpiredPoints(settings: ExpiringPointsSettings,
                          service: PointsService[Task]): Task[Unit] = {
    service.removeExpiredPoints.runEvery(settings.checkEvery)
  }
}
