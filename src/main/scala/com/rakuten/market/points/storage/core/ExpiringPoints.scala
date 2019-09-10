package com.rakuten.market.points.storage.core

import java.time.Instant

import com.rakuten.market.points.data.{Points, PointsTransaction, UserId}

case class ExpiringPoints(transactionId: PointsTransaction.Id,
                          userId: UserId,
                          amount: Points.Amount,
                          expires: Instant)
