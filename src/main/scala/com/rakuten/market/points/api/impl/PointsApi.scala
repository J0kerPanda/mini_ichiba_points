package com.rakuten.market.points.api.impl

import java.time.{Instant, Period}

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.rakuten.market.points.api.core.{Api, EntityNotFound, InvalidRequest, ServiceResult, UnknownServiceError, PointsApiService => CoreApiService}
import com.rakuten.market.points.api.impl.request._
import com.rakuten.market.points.api.impl.response._
import com.typesafe.scalalogging.Logger
import io.circe.{Decoder, Encoder, Printer}
import org.http4s._
import org.http4s.circe.CirceInstances
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import tsec.authentication._


private[api] class PointsApi[F[_]: Sync](val root: String,
                                         auth: JwtAuthService[F],
                                         service: CoreApiService[F]) extends Http4sDsl[F] with Api[F] {

  private val log = Logger[PointsApi[F]]
  private val maxPeriod = Period.ofDays(180)

  private val instances: CirceInstances = CirceInstances.withPrinter(Printer.spaces2).build
  private implicit def decoder[E: Decoder]: EntityDecoder[F, E] = instances.jsonOf
  private implicit def encoder[E: Encoder]: EntityEncoder[F, E] = instances.jsonEncoderOf

  val pointsRoutes: HttpRoutes[F] = auth.service.liftService(TSecAuthService {
    case req @ POST -> Root asAuthed claims =>
      req.request.as[ChangePointsRequest]
        .flatMap { r =>
          wrap(service.changePoints(r.amount, r.expires)(claims.userId))(_ => Ok())
        }

    case GET -> Root / "info" asAuthed claims =>
      service.getPointsInfo(claims.userId).flatMap(Ok(_))

    case GET -> Root / "expiring" asAuthed claims =>
      service.getExpiringPointsInfo(claims.userId).flatMap(points => Ok(ExpiringPointsResponse(points)))
  })

  val transactionRoutes: HttpRoutes[F] = auth.service.liftService(TSecAuthService {
    case GET -> Root / "history" :? FromQPM(from) +& ToQPM(to) asAuthed claims =>
      service.getTransactionHistory(from, to)(claims.userId)
        .flatMap(trs => Ok(TransactionHistoryResponse(trs)))

    case GET -> Root / "history" :? ToQPM(to) asAuthed claims =>
      service.getTransactionHistory(to.minus(maxPeriod), to)(claims.userId)
        .flatMap(trs => Ok(TransactionHistoryResponse(trs)))

    case GET -> Root / "history" :? FromQPM(from) asAuthed claims =>
      service.getTransactionHistory(from, from.plus(maxPeriod))(claims.userId)
        .flatMap(trs => Ok(TransactionHistoryResponse(trs)))

    case req @ POST -> Root / "start" asAuthed claims =>
      req.request.as[TransactPointsRequest]
        .flatMap { r =>
          wrap(service.startPointsTransaction(r.amount, r.expires)(claims.userId))(id => Ok(TransactionStartedResponse(id)))
        }

    case req @ POST -> Root / "cancel" asAuthed claims =>
      req.request.as[TransactionRequest]
        .flatMap { r =>
          wrap(service.cancelPointsTransaction(r.id)(claims.userId))(_ => Ok())
        }

    case req @ POST -> Root / "confirm" asAuthed claims =>
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
    Router(
      (root + "/points") -> pointsRoutes,
      (root + "/points/transaction") -> transactionRoutes,
      root -> testRoutes
    )

  private def wrap[A](result: F[ServiceResult[A]])(f: A => F[Response[F]]): F[Response[F]] =
    result.flatMap {
      case Right(res) => f(res)
      case Left(err) =>
        err match {
          case InvalidRequest => BadRequest()
          case EntityNotFound => NotFound()
          case UnknownServiceError(e) =>
            Sync[F].delay(log.error("API failure", e)) >>
            InternalServerError()
        }
    }
}
