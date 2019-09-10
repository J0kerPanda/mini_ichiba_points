package com.rakuten.market.points.storage.impl

import java.time.Instant

import com.rakuten.market.points.data.{Points, PointsTransaction, UserId}

private[impl] case class ExpiringPoints(transactionId: PointsTransaction.Id,
                                        userId: UserId,
                                        amount: Points.Amount,
                                        expires: Instant)
