package com.rakuten.market.points.storage.impl

import com.rakuten.market.points.data.{PointsInfo, PointsTransaction}
import com.rakuten.market.points.storage.core.ExpiringPoints

private[impl] trait Schema {

  protected implicit val ctx: PostgresContext
  import ctx._

  protected val points =
    quote {
      querySchema[PointsInfo](
        "points",
        _.userId -> "user_id",
        _.total -> "total",
        _.totalExpiring -> "total_expiring",
        _.closestExpiring.map(_.value) -> "closest_expiring_amount",
        _.closestExpiring.map(_.expires) -> "closest_expiring_timestamp"
      )
    }

  protected val pendingTransaction =
    quote {
      querySchema[PointsTransaction.Pending](
        "pending_transaction",
        _.id -> "id",
        _.userId -> "user_id",
        _.amount -> "amount",
        _.time -> "timestamp",
        _.expires -> "expires",
        _.total -> "total",
        _.comment -> "comment"
      )
    }

  protected val confirmedTransaction =
    quote {
      querySchema[PointsTransaction.Confirmed](
        "transaction",
        _.id -> "id",
        _.userId -> "user_id",
        _.amount -> "amount",
        _.time -> "timestamp",
        _.expires -> "expires",
        _.total -> "total",
        _.comment -> "comment"
      )
    }

  protected val expiringPoints =
    quote {
      querySchema[ExpiringPoints](
        "expiring_points",
        _.transactionId -> "transaction_id",
        _.userId -> "user_id",
        _.amount -> "amount",
        _.expires -> "expires"
      )
    }
}
