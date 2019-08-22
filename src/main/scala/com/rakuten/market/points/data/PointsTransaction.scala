package com.rakuten.market.points.data

import java.time.Instant
import java.util.UUID

sealed trait PointsTransaction {
  def userId: UserId
  def time: Instant
  def points: Points
  def total: Points.Amount
  def comment: Option[String]
}

object PointsTransaction {
  type Id = UUID

  case class Unidentified(userId: UserId,
                          time: Instant,
                          points: Points,
                          total: Points.Amount,
                          comment: Option[String]) extends PointsTransaction

  case class Unconfirmed(id: Id,
                         userId: UserId,
                         time: Instant,
                         points: Points,
                         total: Points.Amount,
                         comment: Option[String]) extends PointsTransaction

  case class Confirmed(id: Id,
                       userId: UserId,
                       time: Instant,
                       points: Points,
                       total: Points.Amount,
                       comment: Option[String]) extends PointsTransaction


  case class Cancelled(id: Id,
                       userId: UserId,
                       time: Instant,
                       points: Points,
                       total: Points.Amount,
                       comment: Option[String]) extends PointsTransaction

}

