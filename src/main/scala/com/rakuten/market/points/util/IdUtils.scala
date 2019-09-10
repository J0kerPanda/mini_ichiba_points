package com.rakuten.market.points.util

import java.util.UUID

import cats.effect.Sync
import com.rakuten.market.points.data.PointsTransaction

object IdUtils {

  def generateTransactionId[F[_]: Sync]: F[PointsTransaction.Id] =
    Sync[F].delay(UUID.randomUUID())
}
