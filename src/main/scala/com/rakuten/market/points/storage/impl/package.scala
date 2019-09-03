package com.rakuten.market.points.storage

import java.sql.{Timestamp, Types}
import java.time.Instant
import java.util.UUID

import com.rakuten.market.points.storage.util.PostgresContext
import io.getquill.MappedEncoding

package object impl {

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
