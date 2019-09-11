package com.rakuten.market.points.api.impl.format

import java.time.Instant
import java.util.UUID

private[impl] case class OutputTransaction(id: UUID,
                                           time: Instant,
                                           amount: Int,
                                           expires: Option[Instant],
                                           comment: Option[String])
