package com.rakuten.market.points

import cats.effect.ExitCode
import cats.syntax.flatMap._
import com.rakuten.market.points.api.core.{Api, PointsApiService}
import com.rakuten.market.points.settings.JwtAuthSettings
import com.rakuten.market.points.storage.core.PointsStorage
import com.rakuten.market.points.storage.util.PostgresContext
import io.getquill.context.monix.Runner
import io.getquill.{PostgresMonixJdbcContext, SnakeCase}
import monix.eval.{Task, TaskApp}
import org.flywaydb.core.Flyway
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import tsec.mac.jca.HMACSHA256

import scala.concurrent.duration._

object Application extends TaskApp {

  private lazy implicit val dbCtx: PostgresContext =
    new PostgresMonixJdbcContext(SnakeCase, "db", Runner.default)

  private val pointsStorage = PointsStorage.postgres
  private val authSettings = JwtAuthSettings(
    expiryDuration = 10.minutes,
    signingKey = HMACSHA256.unsafeBuildKey("key".getBytes)
  )

  private val service =
    PointsApiService.default(pointsStorage)

  private val server: Api[Task] =
    Api.points("", authSettings, service)

  override def run(args: List[String]): Task[ExitCode] = {
    migrateDatabase >> runServer
  }

  private def migrateDatabase: Task[Unit] =
    Task.delay {
      Flyway
        .configure()
        .dataSource(dbCtx.dataSource)
        .load()
        .migrate()
    }

  private def runServer: Task[ExitCode] =
    BlazeServerBuilder[Task]
      .bindHttp(8080, "localhost")
      .withHttpApp(Router(server.root -> server.routes).orNotFound)
      .serve
      .compile
      .lastOrError
}
