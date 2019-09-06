package com.rakuten.market.points.api.impl

import cats.effect.Sync
import com.rakuten.market.points.settings.JwtAuthSettings
import io.circe.generic.auto._
import tsec.authentication.{JWTAuthenticator, SecuredRequestHandler}
import tsec.mac.jca.HMACSHA256


private[api] class JwtAuthService[F[_]: Sync](settings: JwtAuthSettings) {

  private val authenticator =
    JWTAuthenticator.pstateless.inBearerToken[F, JwtPayload, HMACSHA256](
      expiryDuration = settings.expiryDuration,
      maxIdle = None,
      signingKey = settings.signingKey
    )

  val service = SecuredRequestHandler(authenticator)
}
