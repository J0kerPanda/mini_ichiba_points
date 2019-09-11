package com.rakuten.market.points.api.impl.request

import java.time.Instant

import com.rakuten.market.points.data.Points

private[impl] case class TransactPointsRequest(amount: Points.Amount,
                                               expires: Option[Instant],
                                               comment: Option[String])
