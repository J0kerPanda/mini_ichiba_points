package com.rakuten.market.points.api.impl.request

import java.time.Instant

import org.http4s.dsl.impl.QueryParamDecoderMatcher

private[impl] object FromQPM extends QueryParamDecoderMatcher[Instant]("from")