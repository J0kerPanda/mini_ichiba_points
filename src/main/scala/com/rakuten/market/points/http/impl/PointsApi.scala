package com.rakuten.market.points.http.impl

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.semigroupk._
import com.rakuten.market.points.data.UserId
import com.rakuten.market.points.http.core.{Api, PointsApiService => CoreApiService}
import com.rakuten.market.points.http.request.{ChangePointsRequest, ConfirmTransactionRequest, TransactPointsRequest}
import com.rakuten.market.points.http.response.TransactionStartedResponse
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder, Printer}
import org.http4s.Credentials.Token
import org.http4s._
import org.http4s.circe.CirceInstances
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.server.{AuthMiddleware, Router}


private[http] class PointsApi[F[_]: Sync](val root: String,
                                          service: CoreApiService[F]) extends Http4sDsl[F] with Api[F] {

  private val instances: CirceInstances = CirceInstances.withPrinter(Printer.spaces2).build
  private implicit def decoder[E: Decoder]: EntityDecoder[F, E] = instances.jsonOf
  private implicit def encoder[E: Encoder]: EntityEncoder[F, E] = instances.jsonEncoderOf

  val userRoutes: AuthedRoutes[UserId, F] = AuthedRoutes.of {
    case GET -> Root / "points" as userId =>
      service.getPointsInfo(userId).flatMap(Ok(_))
  }

  val serviceRoutes: HttpRoutes[F] = HttpRoutes.of {
    case req @ POST -> Root / "change-points" =>
      req.as[ChangePointsRequest]
        .flatMap { r =>
          service.changePoints(r.amount)(r.userId)
        }
        .flatMap {
          case Left(e) => BadRequest()
          case Right(_) => Ok()
        }

    case req @ POST -> Root / "provide-points" =>
      req.as[TransactPointsRequest]
        .flatMap { r =>
          service.startPointsTransaction(r.amount)(r.userId)
        }
        .flatMap {
          case Left(e) => BadRequest()
          case Right(id) => Ok(TransactionStartedResponse(id))
        }

    case req @ POST -> Root / "complete-transaction" =>
      req.as[ConfirmTransactionRequest]
        .flatMap { r =>
          service.confirmPointsTransaction(r.id)
        }
        .flatMap {
          case Left(e) => BadRequest()
          case Right(_) => Ok()
        }
  }

  override val routes: HttpRoutes[F] =
    Router(root -> authUser(userRoutes), root -> serviceRoutes)

  private def authUser: AuthMiddleware[F, UserId] =
    AuthMiddleware {
      Kleisli[OptionT[F, ?], Request[F], UserId] { r =>
        val token = r.headers.collectFirst { case Authorization(Authorization(Token(AuthScheme.Bearer, t))) => t }
        OptionT.fromOption(token).flatMapF(service.authUser)
      }
    }
}
