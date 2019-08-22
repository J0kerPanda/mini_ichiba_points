package com.rakuten.market.points.auth.core

import com.rakuten.market.points.auth.{AuthToken, ServiceToken}
import com.rakuten.market.points.data.UserId

trait AuthService[F[_]] {

  def authUser(token: AuthToken): F[Option[UserId]]

  def authorizePointsAddition(token: ServiceToken): F[Boolean]

  def authorizePointsPayment(token: ServiceToken): F[Boolean]
}
