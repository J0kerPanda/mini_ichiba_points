package com.rakuten.market.points.api.impl

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.rakuten.market.points.api.core.{Api, PointsApiService => CoreApiService}
import com.rakuten.market.points.api.request.{ChangePointsRequest, ConfirmTransactionRequest, TransactPointsRequest}
import com.rakuten.market.points.api.response.TransactionStartedResponse
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

  //todo make a separate format for points info
  val userRoutes: HttpRoutes[F] = auth.service.liftService(TSecAuthService {
    case GET -> Root / "points" asAuthed claims =>
      service.getPointsInfo(claims.userId).flatMap(Ok(_))

    case req @ POST -> Root / "points" asAuthed _ =>
      req.request.as[ChangePointsRequest]
        .flatMap { r =>
          service.changePoints(r.amount)(r.userId)
        }
        .flatMap {
          case Left(e) => BadRequest()
          case Right(_) => Ok()
        }

    case req @ POST -> Root / "transaction" / "start" asAuthed _ =>
      req.request.as[TransactPointsRequest]
        .flatMap { r =>
          service.startPointsTransaction(r.amount)(r.userId)
        }
        .flatMap {
          case Left(e) => BadRequest()
          case Right(id) => Ok(TransactionStartedResponse(id))
        }

    case req @ POST -> Root / "transaction" / "confirm"  asAuthed _ =>
      req.request.as[ConfirmTransactionRequest]
        .flatMap { r =>
          service.confirmPointsTransaction(r.id)
        }
        .flatMap {
          case Left(e) => BadRequest()
          case Right(_) => Ok()
        }
  })

  val testRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "test" =>
      Ok()
  }

  override val routes: HttpRoutes[F] =
    Router(root -> userRoutes, root -> testRoutes)
}
