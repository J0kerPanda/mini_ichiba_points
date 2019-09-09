package com.rakuten.market.points

import cats.effect.ExitCode
import com.rakuten.market.points.api.core.{Api, PointsApiService}
import com.rakuten.market.points.settings.{ApiSettings, ServerSettings}
import com.rakuten.market.points.storage.core.PointsStorage
import com.rakuten.market.points.storage.util.PostgresContext
import io.getquill.context.monix.Runner
import io.getquill.{PostgresMonixJdbcContext, SnakeCase}
import monix.eval.{Task, TaskApp}
import org.flywaydb.core.Flyway
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import pureconfig.generic.ProductHint
import pureconfig.{CamelCase, ConfigFieldMapping}

object Application extends TaskApp {

  private lazy implicit val dbCtx: PostgresContext =
    new PostgresMonixJdbcContext(SnakeCase, "db", Runner.default)

  override def run(args: List[String]): Task[ExitCode] =
    for {
      apiConfig <- apiSettings
      _ <- migrateDatabase
      pointsStorage = PointsStorage.postgres
      key = apiConfig.auth.signingKey.toJavaKey
      _ = println("encoded", key.getFormat, new String(key.getEncoded))
      service = PointsApiService.default(pointsStorage)
      api = Api.points("", apiConfig.auth, service)
      exitCode <- runServer(api, apiConfig.server)
    } yield exitCode

  private def migrateDatabase: Task[Unit] =
    Task.delay {
      Flyway
        .configure()
        .dataSource(dbCtx.dataSource)
        .load()
        .migrate()
    }

  private def runServer(api: Api[Task], settings: ServerSettings): Task[ExitCode] =
    BlazeServerBuilder[Task]
      .bindHttp(settings.port, settings.host)
      .withHttpApp(Router(api.root -> api.routes).orNotFound)
      .serve
      .compile
      .lastOrError

  private def apiSettings: Task[ApiSettings] = {
    import pureconfig.generic.auto._
    import com.rakuten.market.points.settings.AuthSettings.signingKeyReader

    implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))
    Task.delay(pureconfig.loadConfigOrThrow[ApiSettings]("api"))
  }
}
