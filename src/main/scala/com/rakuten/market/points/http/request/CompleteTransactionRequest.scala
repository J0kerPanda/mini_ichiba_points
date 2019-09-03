package com.rakuten.market.points.http.request

import com.rakuten.market.points.data.PointsTransaction

private[http] case class CompleteTransactionRequest(id: PointsTransaction.Id)
