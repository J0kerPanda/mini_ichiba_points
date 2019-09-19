package com.rakuten.market.points.api.impl

import java.time.Period

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.rakuten.market.points.api.core.Api
import com.rakuten.market.points.api.impl.request._
import com.rakuten.market.points.api.impl.response._
import com.rakuten.market.points.data.PointsTransaction
import com.rakuten.market.points.service.core._
import com.rakuten.market.points.settings.CorsSettings
import com.typesafe.scalalogging.Logger
import io.circe.{Decoder, Encoder, Printer}
import org.http4s._
import org.http4s.circe.CirceInstances
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.server.middleware.{CORS, CORSConfig}
import tsec.authentication._

import scala.concurrent.duration._



private[api] class PointsApi[F[_]: Sync](val root: String,
                                         corsSettings: CorsSettings,
                                         auth: JwtAuthService[F],
                                         service: PointsService[F]) extends Http4sDsl[F] with Api[F] {

  private val log = Logger[PointsApi[F]]
  private val maxPeriod = Period.ofDays(180)
  private val corsConfig = CORSConfig(
    anyOrigin = false,
    allowCredentials = true,
    maxAge = 1.day.toSeconds,
    allowedOrigins = corsSettings.origins.toSet
  )

  private val instances: CirceInstances = CirceInstances.withPrinter(Printer.spaces2).build
  private implicit def decoder[E: Decoder]: EntityDecoder[F, E] = instances.jsonOf
  private implicit def encoder[E: Encoder]: EntityEncoder[F, E] = instances.jsonEncoderOf

  val pointsRoutes: HttpRoutes[F] = auth.service.liftService(TSecAuthService {
    case req @ POST -> Root asAuthed claims =>
      process[ChangePointsRequest, Unit](req.request)
        { r => service.changePoints(r.amount, r.expires, r.comment)(claims.userId) }
        { _ => Ok() }

    case GET -> Root / "info" asAuthed claims =>
      service.getPointsInfo(claims.userId)
        .flatMap(Ok(_))

    case GET -> Root / "expiring" asAuthed claims =>
      service.getExpiringPointsInfo(claims.userId)
        .flatMap(points => Ok(ExpiringPointsResponse(points)))
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
      process[TransactPointsRequest, PointsTransaction.Id](req.request)
        { r => service.startPointsTransaction(r.amount, r.expires, r.comment)(claims.userId) }
        { id => Ok(TransactionStartedResponse(id)) }

    case req @ POST -> Root / "cancel" asAuthed claims =>
      process[TransactionRequest, Unit](req.request)
        { r => service.cancelPointsTransaction(r.id)(claims.userId) }
        { _ => Ok() }

    case req @ POST -> Root / "confirm" asAuthed claims =>
      process[TransactionRequest, Unit](req.request)
        { r => service.confirmPointsTransaction(r.id)(claims.userId) }
        { _ => Ok() }
  })

  val testRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "test" =>
      Ok()
  }

  override val routes: HttpRoutes[F] =
    CORS(
      http = Router(
        (root + "/points") -> pointsRoutes,
        (root + "/points/transaction") -> transactionRoutes,
        root -> testRoutes
      ),
      config = corsConfig
    )

  private def process[A, B](req: Request[F])
                           (handle: A => F[ServiceResult[B]])
                           (wrap: B => F[Response[F]])
                           (implicit D: EntityDecoder[F, A]): F[Response[F]] =
    req.attemptAs[A].foldF(
      e =>  Sync[F].delay(log.error("Bad request", e)) >> BadRequest(),
      decoded => handle(decoded).flatMap {
        case Right(res) => wrap(res)
        case Left(err) =>
          err match {
            case InvalidRequest => BadRequest()
            case EntityNotFound => NotFound()
            case UnknownServiceError(e) =>
              Sync[F].delay(log.error("API failure", e)) >>
              InternalServerError()
          }
      }
    )
}
