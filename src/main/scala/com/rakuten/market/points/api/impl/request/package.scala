package com.rakuten.market.points.api.impl

import java.time.Instant

import org.http4s.QueryParamDecoder

package object request {

  implicit val instantParamDecoder: QueryParamDecoder[Instant] =
    QueryParamDecoder[String].map(Instant.parse)
}