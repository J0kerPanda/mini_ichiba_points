package com.rakuten.market.points.http.core

import com.rakuten.market.points.http.impl.PointsApi
import monix.eval.Task
import org.http4s.HttpRoutes

object Api {

  def points(root: String, service: PointsApiService[Task]): Api[Task] =
    new PointsApi(root, service)
}

trait Api[F[_]] {

  def root: String
  def routes: HttpRoutes[F]
}
