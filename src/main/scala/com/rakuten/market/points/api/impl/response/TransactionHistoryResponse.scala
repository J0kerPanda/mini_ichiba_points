package com.rakuten.market.points.api.impl.response

import com.rakuten.market.points.data.PointsTransaction

private[impl] case class TransactionHistoryResponse(transactions: List[PointsTransaction.Confirmed])
