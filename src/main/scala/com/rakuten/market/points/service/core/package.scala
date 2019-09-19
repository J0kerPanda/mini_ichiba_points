package com.rakuten.market.points.service

package object core {

  object ServiceResult {
    def error[A](e: ServiceError): ServiceResult[A] =
      Left(e)
  }

  type ServiceResult[A] = Either[ServiceError, A]
}
