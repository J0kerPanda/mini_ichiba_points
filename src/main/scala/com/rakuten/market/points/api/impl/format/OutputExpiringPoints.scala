package com.rakuten.market.points.api.impl.format

import java.time.Instant

private[impl] case class OutputExpiringPoints(amount: Int, expires: Instant)
