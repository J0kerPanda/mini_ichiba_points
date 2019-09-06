package com.rakuten.market.points.api.request

import com.rakuten.market.points.data.{Points, UserId}

private[api] case class TransactPointsRequest(userId: UserId, amount: Points.Amount)
