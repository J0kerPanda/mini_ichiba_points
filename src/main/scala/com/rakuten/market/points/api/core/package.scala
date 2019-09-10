package com.rakuten.market.points.api

package object core {
  type ServiceResult[A] = Either[ServiceError, A]
}
