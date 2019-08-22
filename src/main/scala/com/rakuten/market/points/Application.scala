package com.rakuten.market.points

import cats.effect.ExitCode
import com.rakuten.market.points.http.core.Api
import com.rakuten.market.points.http.impl.PointsApi
import monix.eval.{Task, TaskApp}
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder


object Application extends TaskApp {

  private val service = Mocks.service
  private val server: Api[Task] = new PointsApi("", service)

  override def run(args: List[String]): Task[ExitCode] = {
    BlazeServerBuilder[Task]
      .bindHttp(8080, "localhost")
      .withHttpApp(Router(server.root -> server.routes).orNotFound)
      .serve
      .compile
      .lastOrError
  }
}
