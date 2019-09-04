package com.rakuten.market.points.http.request

import com.rakuten.market.points.data.{Points, UserId}

private[http] case class ChangePointsRequest(userId: UserId, amount: Points.Amount)
