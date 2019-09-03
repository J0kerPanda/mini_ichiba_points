package com.rakuten.market.points.http.impl

import cats.data.{Kleisli, OptionT}
import cats.syntax.functor._
import com.rakuten.market.points.data.UserId
import com.rakuten.market.points.http.core.{Api, PointsApiService => CoreApiService}
import monix.eval.Task
import org.http4s.Credentials.Token
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthScheme, AuthedRoutes, HttpRoutes, Request}

private[http] class PointsApi(val root: String,
                              service: CoreApiService[Task]) extends Http4sDsl[Task] with Api[Task] {


  val userRoutes: AuthedRoutes[UserId, Task] = AuthedRoutes.of {
    case GET -> Root / "points" as userId =>
      service.getPointsInfo(userId).flatMap(_ => Ok(()))
  }

  override val routes: HttpRoutes[Task] =
    Router(root -> authUser(userRoutes))

  private def authUser: AuthMiddleware[Task, UserId] =
    AuthMiddleware {
      Kleisli[OptionT[Task, ?], Request[Task], UserId] { r =>
        val token = r.headers.collectFirst { case Authorization(Authorization(Token(AuthScheme.Bearer, t))) => t }
        OptionT.fromOption[Task](token).flatMapF(service.authUser)
      }
    }

}
