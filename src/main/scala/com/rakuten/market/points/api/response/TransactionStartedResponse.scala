package com.rakuten.market.points.api.response

import com.rakuten.market.points.data.PointsTransaction

private[api] case class TransactionStartedResponse(transactionId: PointsTransaction.Id)
