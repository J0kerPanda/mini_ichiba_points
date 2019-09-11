package com.rakuten.market.points.api.impl

import cats.effect.Sync
import com.rakuten.market.points.settings.AuthSettings
import io.circe.generic.auto._
import tsec.authentication.{JWTAuthenticator, SecuredRequestHandler}
import tsec.mac.jca.HMACSHA256

private[api] class JwtAuthService[F[_]: Sync](val settings: AuthSettings) {

  private val authenticator =
    JWTAuthenticator.pstateless.inBearerToken[F, JwtClaims, HMACSHA256](
      expiryDuration = settings.expirationInterval,
      maxIdle = None,
      signingKey = settings.signingKey
    )

  val service = SecuredRequestHandler(authenticator)
}
