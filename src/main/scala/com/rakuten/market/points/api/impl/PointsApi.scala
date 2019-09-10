package com.rakuten.market.points.api.impl

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.rakuten.market.points.api.core.{Api, EntityNotFound, InvalidRequest, ServiceResult, UnknownServiceError}
import com.rakuten.market.points.api.core.{PointsApiService => CoreApiService}
import com.rakuten.market.points.api.impl.request._
import com.rakuten.market.points.api.impl.response._
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder, Printer}
import org.http4s._
import org.http4s.circe.CirceInstances
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import tsec.authentication._


private[api] class PointsApi[F[_]: Sync](val root: String,
                                         auth: JwtAuthService[F],
                                         service: CoreApiService[F]) extends Http4sDsl[F] with Api[F] {

  private val instances: CirceInstances = CirceInstances.withPrinter(Printer.spaces2).build
  private implicit def decoder[E: Decoder]: EntityDecoder[F, E] = instances.jsonOf
  private implicit def encoder[E: Encoder]: EntityEncoder[F, E] = instances.jsonEncoderOf

  val userRoutes: HttpRoutes[F] = auth.service.liftService(TSecAuthService {
    case req @ POST -> Root / "points" asAuthed claims =>
      req.request.as[ChangePointsRequest]
        .flatMap { r =>
          wrap(service.changePoints(r.amount, r.expires)(claims.userId))(_ => Ok())
        }

    case GET -> Root / "points" / "info" asAuthed claims =>
      service.getPointsInfo(claims.userId).flatMap(Ok(_))

    case GET -> Root / "points" / "expiring" asAuthed claims =>
      service.getExpiringPointsInfo(claims.userId).flatMap(points => Ok(ExpiringPointsResponse(points)))

    case GET -> Root / "points" / "history" asAuthed claims =>
      service.getTransactionHistory(???, ???)(claims.userId)
        .flatMap(trs => Ok(TransactionHistoryResponse(trs)))

    case req @ POST -> Root / "transaction" / "start" asAuthed claims =>
      req.request.as[TransactPointsRequest]
        .flatMap { r =>
          wrap(service.startPointsTransaction(r.amount, r.expires)(claims.userId))(id => Ok(TransactionStartedResponse(id)))
        }

    case req @ POST -> Root / "transaction" / "cancel" asAuthed claims =>
      req.request.as[TransactionRequest]
        .flatMap { r =>
          wrap(service.cancelPointsTransaction(r.id)(claims.userId))(_ => Ok())
        }

    case req @ POST -> Root / "transaction" / "confirm" asAuthed claims =>
      req.request.as[TransactionRequest]
        .flatMap { r =>
          wrap(service.confirmPointsTransaction(r.id)(claims.userId))(_ => Ok())
        }
  })

  val testRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "test" =>
      Ok()
  }

  override val routes: HttpRoutes[F] =
    Router(root -> userRoutes, root -> testRoutes)

  //todo log errors?
  private def wrap[A](result: F[ServiceResult[A]])(f: A => F[Response[F]]): F[Response[F]] =
    result.flatMap {
      case Right(res) => f(res)
      case Left(err) =>
        err match {
          case InvalidRequest => BadRequest()
          case EntityNotFound => NotFound()
          case UnknownServiceError(_) => InternalServerError()
        }
    }
}
