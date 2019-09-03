package com.rakuten.market.points.http.impl

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.rakuten.market.points.data.UserId
import com.rakuten.market.points.http.core.{Api, PointsApiService => CoreApiService}
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder, Printer}
import org.http4s.Credentials.Token
import org.http4s.circe.CirceInstances
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthScheme, AuthedRoutes, HttpRoutes, Request, _}


private[http] class PointsApi[F[_]: Sync](val root: String,
                                          service: CoreApiService[F]) extends Http4sDsl[F] with Api[F] {

  private val instances: CirceInstances = CirceInstances.withPrinter(Printer.spaces2).build
  private implicit def decoder[E: Decoder]: EntityDecoder[F, E] = instances.jsonOf
  private implicit def encoder[E: Encoder]: EntityEncoder[F, E] = instances.jsonEncoderOf

  val userRoutes: AuthedRoutes[UserId, F] = AuthedRoutes.of {
    case GET -> Root / "points" as userId =>
      service.getPointsInfo(userId).flatMap(Ok(_))
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

  private def authAddPoints: AuthMiddleware[F, Unit] =
    AuthMiddleware {
      Kleisli[OptionT[F, ?], Request[F], Unit] { r =>
        //todo
        OptionT.pure(())
      }
    }

}
