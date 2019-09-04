package com.rakuten.market.points.data

import java.time.Instant
import java.util.UUID

sealed trait PointsTransaction {
  def userId: UserId
  def time: Instant
  def amount: Points.Amount
  def expires: Option[Instant]
  def total: Points.Amount
  def comment: Option[String]
}

object PointsTransaction {
  type Id = UUID

  case class Pending(id: Id,
                     userId: UserId,
                     time: Instant,
                     amount: Points.Amount,
                     expires: Option[Instant],
                     total: Points.Amount,
                     comment: Option[String]) extends PointsTransaction

  case class Confirmed(id: Id,
                       userId: UserId,
                       time: Instant,
                       amount: Points.Amount,
                       expires: Option[Instant],
                       total: Points.Amount,
                       comment: Option[String]) extends PointsTransaction
}

