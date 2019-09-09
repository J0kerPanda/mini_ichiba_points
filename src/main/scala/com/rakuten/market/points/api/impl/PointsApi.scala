package com.rakuten.market.points.api.impl

import java.util.UUID

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
import tsec.jws.mac.JWTMac
import tsec.jwt.JWTClaims

import scala.concurrent.duration._

import cats.Id
import org.http4s.HttpService
import org.http4s.dsl.io._
import tsec.authentication._
import tsec.common.SecureRandomId
import tsec.mac.jca.{HMACSHA256, MacSigningKey}
import scala.concurrent.duration._
import io.circe.syntax._


private[api] class PointsApi[F[_]: Sync](val root: String,
                                         auth: JwtAuthService[F],
                                         service: CoreApiService[F]) extends Http4sDsl[F] with Api[F] {

  private val instances: CirceInstances = CirceInstances.withPrinter(Printer.spaces2).build
  private implicit def decoder[E: Decoder]: EntityDecoder[F, E] = instances.jsonOf
  private implicit def encoder[E: Encoder]: EntityEncoder[F, E] = instances.jsonEncoderOf


  val data = JwtClaims(UUID.randomUUID())

  private val h: F[(JWTMac[HMACSHA256], JwtClaims)] =
    for {
      claims <- JWTClaims
        .withDuration[F](
        expiration = Some(10.minutes),
        customFields = List("userId" -> data.asJson)
      )
      _ = println(claims.getCustom[JwtClaims]("userId"))
      key = auth.settings.signingKey
      _ = println(key.toJavaKey)
      stringjwt       <- JWTMac.buildToString[F, HMACSHA256](claims, key) //Or build it straight to string
      _ = println(stringjwt)
      claims2      <- auth.service.authenticator.create(data)
      stringjwt2      = JWTMac.toEncodedString[F, HMACSHA256](claims2.jwt)
      _ = println(stringjwt2)
      parsed          <- JWTMac.verifyAndParse[F, HMACSHA256](stringjwt, key)
      doge            <- parsed.body.getCustomF[F, JwtClaims]("userId")
      _ = println(doge)
    } yield (parsed, doge)

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
      h.flatMap(_ => Ok())
  }

  override val routes: HttpRoutes[F] =
    Router(root -> userRoutes, root -> testRoutes)
}
