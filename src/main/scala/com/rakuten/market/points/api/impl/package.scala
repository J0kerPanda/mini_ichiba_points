package com.rakuten.market.points.api

import com.rakuten.market.points.api.impl.format.{OutputExpiringPoints, OutputPointsInfo, OutputTransaction}
import com.rakuten.market.points.data.{Points, PointsInfo, PointsTransaction}
import io.circe.Encoder
import io.circe.generic.AutoDerivation
import io.circe.generic.semiauto._

package object impl extends AutoDerivation {

  implicit val outputExpiringPointsEncoder: Encoder[OutputExpiringPoints] =
    deriveEncoder[OutputExpiringPoints]

  implicit val transactionsEncoder: Encoder[PointsTransaction.Confirmed] =
    deriveEncoder[OutputTransaction].contramap(t =>
      OutputTransaction(
        id = t.id,
        time = t.time,
        amount = t.amount,
        expires = t.expires,
        comment = t.comment
      )
    )

  implicit val expiringPointsEncoder: Encoder[Points.Expiring] =
    deriveEncoder[OutputExpiringPoints].contramap(e =>
      OutputExpiringPoints(
        amount = e.value,
        expires = e.expires
      )
    )

  implicit val pointsInfo: Encoder[PointsInfo] =
    deriveEncoder[OutputPointsInfo].contramap(i =>
      OutputPointsInfo(
        total = i.total,
        totalExpiring = i.totalExpiring,
        closestExpiring = i.closestExpiring.map(e => OutputExpiringPoints(e.value, e.expires)))
    )
}
