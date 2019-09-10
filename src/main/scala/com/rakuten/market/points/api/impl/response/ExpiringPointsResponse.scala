package com.rakuten.market.points.api.impl.response

import com.rakuten.market.points.data.Points

private[impl] case class ExpiringPointsResponse(points: List[Points.Expiring])
