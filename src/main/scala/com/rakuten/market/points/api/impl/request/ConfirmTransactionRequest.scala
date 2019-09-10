package com.rakuten.market.points.api.impl.request

import com.rakuten.market.points.data.PointsTransaction

private[impl] case class ConfirmTransactionRequest(id: PointsTransaction.Id)
