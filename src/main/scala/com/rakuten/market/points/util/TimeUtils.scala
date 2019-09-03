package com.rakuten.market.points.util

import java.time.Instant

import cats.effect.Sync

object TimeUtils {

  def serverTime[F[_]: Sync]: F[Instant] =
    Sync[F].delay(Instant.now())
}
