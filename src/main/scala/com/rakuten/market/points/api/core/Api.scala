package com.rakuten.market.points.api.core

import com.rakuten.market.points.api.impl.{JwtAuthService, PointsApi}
import com.rakuten.market.points.settings.JwtAuthSettings
import monix.eval.Task
import org.http4s.HttpRoutes

object Api {

  def points(root: String, settings: JwtAuthSettings, service: PointsApiService[Task]): Api[Task] =
    new PointsApi(root, new JwtAuthService(settings), service)
}

trait Api[F[_]] {

  def root: String
  def routes: HttpRoutes[F]
}
