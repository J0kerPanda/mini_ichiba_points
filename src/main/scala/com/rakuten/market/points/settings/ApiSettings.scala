package com.rakuten.market.points.settings

case class ApiSettings(auth: AuthSettings,
                       server: ServerSettings,
                       cors: CorsSettings)
