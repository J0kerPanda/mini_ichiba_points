package com.rakuten.market.points.storage.impl

import java.time.Instant

private[impl] trait Quotes {

  protected implicit val ctx: PostgresContext

  implicit class InstantQuotes(left: Instant) {
    import ctx._
    def >=(right: Instant) = quote(infix"$left >= $right".as[Boolean])

    def <=(right: Instant) = quote(infix"$left <= $right".as[Boolean])
  }
}
