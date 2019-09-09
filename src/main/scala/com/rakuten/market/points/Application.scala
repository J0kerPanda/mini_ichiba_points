package com.rakuten.market.points

import cats.effect.ExitCode
import cats.syntax.flatMap._
import com.rakuten.market.points.api.core.{Api, PointsApiService}
import com.rakuten.market.points.settings.JwtAuthSettings
import com.rakuten.market.points.storage.core.PointsStorage
import com.rakuten.market.points.storage.util.PostgresContext
import io.getquill.context.monix.Runner
import io.getquill.{PostgresMonixJdbcContext, SnakeCase}
import javax.crypto.spec.SecretKeySpec
import monix.eval.{Task, TaskApp}
import org.flywaydb.core.Flyway
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import tsec.mac.jca.{HMACSHA256, MacSigningKey}

import scala.concurrent.duration._

object Application extends TaskApp {

  private lazy implicit val dbCtx: PostgresContext =
    new PostgresMonixJdbcContext(SnakeCase, "db", Runner.default)

  private val pointsStorage = PointsStorage.postgres

  val key = new SecretKeySpec("qwertyuiopasdfghjklzxcvbnm123456".getBytes, "HS256")
  private val authSettings = JwtAuthSettings(
    expiryDuration = 10.minutes,
    signingKey =
      MacSigningKey[HMACSHA256](key)
  )

  println("eocnded", key.getFormat, new String(key.getEncoded))

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
