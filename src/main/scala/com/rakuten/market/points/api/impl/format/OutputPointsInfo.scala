package com.rakuten.market.points.api.impl.format

private[impl] case class OutputPointsInfo(total: Int,
                                          totalExpiring: Int,
                                          closestExpiring: Option[OutputExpiringPoints])
