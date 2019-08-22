package com.rakuten.market.points.http.impl

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.rakuten.market.points.data.UserId
import com.rakuten.market.points.http.core.{Api, ApiService}
import org.http4s.Credentials.Token
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthScheme, AuthedRoutes, HttpRoutes, Request}

class PointsApi[F[_]: Sync](val root: String,
                            service: ApiService[F]) extends Http4sDsl[F] with Api[F] {


  val userRoutes: AuthedRoutes[UserId, F] = AuthedRoutes.of {
    case GET -> Root / "points" as userId =>
      service.getInfo(userId).flatMap(_ => Ok(()))
  }

  override val routes: HttpRoutes[F] =
    Router(root -> authUser(userRoutes))

  private def authUser: AuthMiddleware[F, UserId] =
    AuthMiddleware {
      Kleisli[OptionT[F, ?], Request[F], UserId] { r =>
        val token = r.headers.collectFirst { case Authorization(Authorization(Token(AuthScheme.Bearer, t))) => t }
        OptionT.fromOption(token).flatMapF(service.authUser)
      }
    }

}
