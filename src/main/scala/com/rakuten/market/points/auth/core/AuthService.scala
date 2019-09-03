package com.rakuten.market.points.auth.core

import com.rakuten.market.points.auth.{AuthToken, ServiceToken}
import com.rakuten.market.points.data.UserId
import com.rakuten.market.points.auth.impl.{AuthService => DefaultAuthService}
import monix.eval.Task

object AuthService {

  def default: AuthService[Task] =
    new DefaultAuthService()
}

trait AuthService[F[_]] {

  def authUser(token: AuthToken): F[Option[UserId]]

  def authorizeProvidingPoints(token: ServiceToken): F[Boolean]

  def authorizePointsPayment(token: ServiceToken): F[Boolean]
}
