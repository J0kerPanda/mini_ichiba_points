package com.rakuten.market.points.storage

import java.sql.{Timestamp, Types}
import java.time.Instant
import java.util.UUID

import io.getquill.{MappedEncoding, PostgresMonixJdbcContext, SnakeCase}

package object impl {

  type PostgresContext = PostgresMonixJdbcContext[SnakeCase]

  implicit val encodeUUID: MappedEncoding[UUID, String] =
    MappedEncoding[UUID, String](_.toString)
  implicit val decodeUUID: MappedEncoding[String, UUID] =
    MappedEncoding[String, UUID](UUID.fromString)

  implicit def encodeInstant(implicit ctx: PostgresContext): ctx.Encoder[Instant] =
    ctx.encoder(
      Types.TIMESTAMP,
      (i, ts, r) => r.setTimestamp(i, Timestamp.from(ts))
    )

  implicit def decodeInstant(implicit ctx: PostgresContext): ctx.Decoder[Instant] =
    ctx.decoder((i, r) => r.getTimestamp(i).toInstant)
}
