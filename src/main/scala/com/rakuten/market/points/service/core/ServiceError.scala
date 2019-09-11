package com.rakuten.market.points.service.core

sealed trait ServiceError

case object InvalidRequest extends ServiceError
case object EntityNotFound extends ServiceError
case class UnknownServiceError(t: Throwable) extends ServiceError