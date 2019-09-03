package com.rakuten.market.points.auth.impl

import java.util.UUID

import com.rakuten.market.points.auth.{AuthToken, ServiceToken}
import com.rakuten.market.points.auth.core.{AuthService => CoreAuthService}
import com.rakuten.market.points.data.UserId
import monix.eval.Task

private[auth] class AuthService extends CoreAuthService[Task] {

  override def authUser(token: AuthToken): Task[Option[UserId]] =
    Task.pure(Some(UUID.fromString("b982ba4a-ce23-11e9-a32f-2a2ae2dbcce4")))

  override def authorizeProvidingPoints(token: ServiceToken): Task[Boolean] =
    Task.pure(true)

  override def authorizePointsPayment(token: ServiceToken): Task[Boolean] =
    Task.pure(true)
}
