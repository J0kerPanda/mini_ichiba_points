package com.rakuten.market.points

import cats.effect.ExitCode
import cats.syntax.flatMap._
import com.rakuten.market.points.auth.core.AuthService
import com.rakuten.market.points.http.core.{Api, PointsApiService}
import com.rakuten.market.points.storage.core.PointsStorage
import com.rakuten.market.points.storage.util.PostgresContext
import io.getquill.context.monix.Runner
import io.getquill.{PostgresMonixJdbcContext, SnakeCase}
import monix.eval.{Task, TaskApp}
import org.flywaydb.core.Flyway
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder


object Application extends TaskApp {

  private lazy implicit val dbCtx: PostgresContext =
    new PostgresMonixJdbcContext(SnakeCase, "db", Runner.default)

  private val pointsStorage = PointsStorage.postgres
  private val authService = AuthService.default

  private val service =
    PointsApiService.default(authService, pointsStorage)

  private val server: Api[Task] =
    Api.points("", service)

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
