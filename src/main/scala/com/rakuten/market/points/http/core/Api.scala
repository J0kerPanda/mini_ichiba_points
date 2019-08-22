package com.rakuten.market.points.http.core

import org.http4s.HttpRoutes

trait Api[F[_]] {

  def root: String
  def routes: HttpRoutes[F]
}
