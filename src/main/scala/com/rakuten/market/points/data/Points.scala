package com.rakuten.market.points.data

import java.time.Instant

import com.rakuten.market.points.data.Points.Amount
import io.getquill.Embedded

object Points {
  type Amount = Int

  case class Simple(value: Amount) extends AnyVal with Points
  case class Expiring(value: Amount, expires: Instant) extends Points with Embedded
}

/** Value classes can only extend universal traits -> "extends Any"
  */
sealed trait Points extends Any {
  def value: Amount
}
