package com.rakuten.market.points.storage.impl

import com.rakuten.market.points.data.{PointsInfo, PointsTransaction}

private[impl] object Schema {

  def points(implicit ctx: PostgresContext) = {
    import ctx._
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
  }

  def pendingTransacton(implicit ctx: PostgresContext) = {
    import ctx._
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
  }

  def confirmedTransaction(implicit ctx: PostgresContext) = {
    import ctx._
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
  }
}
