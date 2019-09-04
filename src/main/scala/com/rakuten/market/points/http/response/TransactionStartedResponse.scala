package com.rakuten.market.points.http.response

import com.rakuten.market.points.data.PointsTransaction

private[http] case class TransactionStartedResponse(transactionId: PointsTransaction.Id)
