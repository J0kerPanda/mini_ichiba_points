package com.rakuten.market.points.api.request

import com.rakuten.market.points.data.{Points, UserId}

private[api] case class ChangePointsRequest(userId: UserId, amount: Points.Amount)
