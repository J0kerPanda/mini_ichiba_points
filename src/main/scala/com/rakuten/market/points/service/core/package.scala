package com.rakuten.market.points.service

package object core {
  type ServiceResult[A] = Either[ServiceError, A]
}
