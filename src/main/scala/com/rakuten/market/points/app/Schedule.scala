package com.rakuten.market.points.app

import com.rakuten.market.points.settings.TransactionSettings
import com.rakuten.market.points.storage.core.PointsStorage
import com.rakuten.market.points.util.TimeUtils
import monix.eval.Task
import cats.syntax.flatMap._

private[app] object Schedule {



  def removePendingTransactions(settings: TransactionSettings,
                                storage: PointsStorage[Task]): Task[Unit] = {
    val job = for {
      time <- TimeUtils.serverTime[Task]
      from = time.minusMillis(settings.expirationInterval.toMillis)
      res <- storage.removePendingTransactions(from)
    } yield res

    job >> Task.sleep(settings.checkEvery) >> removePendingTransactions(settings, storage)
  }
}
