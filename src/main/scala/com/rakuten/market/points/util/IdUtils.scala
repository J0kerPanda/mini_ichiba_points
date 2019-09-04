package com.rakuten.market.points.util

import java.util.UUID

import com.rakuten.market.points.data.PointsTransaction
import monix.eval.Task

object IdUtils {

  def generateTransactionId: Task[PointsTransaction.Id] =
    Task.delay(UUID.randomUUID())
}
