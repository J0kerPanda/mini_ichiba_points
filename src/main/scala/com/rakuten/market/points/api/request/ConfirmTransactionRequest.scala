package com.rakuten.market.points.api.request

import com.rakuten.market.points.data.PointsTransaction

private[api] case class ConfirmTransactionRequest(id: PointsTransaction.Id)
