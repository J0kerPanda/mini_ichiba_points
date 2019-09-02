package com.rakuten.market.points.storage

import io.getquill.{PostgresMonixJdbcContext, SnakeCase}

package object util {
  type PostgresContext = PostgresMonixJdbcContext[SnakeCase]
}
