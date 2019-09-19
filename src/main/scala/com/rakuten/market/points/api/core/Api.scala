package com.rakuten.market.points.api.core

import com.rakuten.market.points.api.impl.{JwtAuthService, PointsApi}
import com.rakuten.market.points.service.core.PointsService
import com.rakuten.market.points.settings.{AuthSettings, CorsSettings}
import monix.eval.Task
import org.http4s.HttpRoutes

object Api {

  def points(root: String,
             corsSettings: CorsSettings,
             authSettings: AuthSettings,
             service: PointsService[Task]): Api[Task] =
    new PointsApi(root, corsSettings, new JwtAuthService(authSettings), service)
}

trait Api[F[_]] {

  def root: String
  def routes: HttpRoutes[F]
}
