package com.rakuten.market.points.algebra.core

import java.time.Instant

import com.rakuten.market.points.data.{PointsTransaction, UserId}

trait PointsTransactionOps[F[_]] {

  def init(transaction: PointsTransaction.Unidentified): F[PointsTransaction.Unconfirmed]

  def confirm(transaction: PointsTransaction.Unconfirmed): F[PointsTransaction.Confirmed]

  def cancel(transaction: PointsTransaction.Unconfirmed): F[PointsTransaction.Cancelled]

  //todo filters object? -> expiring only?
  def getHistory(id: UserId, from: Instant, to: Instant): F[List[PointsTransaction.Confirmed]]
}
