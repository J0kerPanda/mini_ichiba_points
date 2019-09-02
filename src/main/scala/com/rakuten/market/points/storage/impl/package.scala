package com.rakuten.market.points.storage

import java.time.Instant
import java.util.UUID

import io.getquill.MappedEncoding

package object impl {

  implicit val encodeUUID: MappedEncoding[UUID, String] =
    MappedEncoding[UUID, String](_.toString)
  implicit val decodeUUID: MappedEncoding[String, UUID] =
    MappedEncoding[String, UUID](UUID.fromString)

  implicit val encodeInstant: MappedEncoding[Instant, Long] =
    MappedEncoding[Instant, Long](_.toEpochMilli)
  implicit val decodeInstant: MappedEncoding[Long, Instant] =
    MappedEncoding[Long, Instant](Instant.ofEpochMilli)
}
