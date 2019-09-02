package com.rakuten.market.points.storage.impl

import com.rakuten.market.points.data.PointsInfo
import com.rakuten.market.points.storage.util.PostgresContext
import Formats._

private[impl] object Schema {

  def points(implicit ctx: PostgresContext) = {
    import ctx._
    quote {
      querySchema[PointsInfo](
        "points",
        _.userId -> "user_id",
        _.total -> "total",
        _.totalExpiring -> "total_expiring",
        _.closestExpiring.map(_.value) -> "closest_expiring_value",
        _.closestExpiring.map(_.expires) -> "closest_expiring_timestamp"
      )
    }
  }
}
