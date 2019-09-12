package com.rakuten.market.points


import com.rakuten.market.points.settings.ApplicationSettings

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration.Duration

import java.time.Clock

import io.circe.{Json, Encoder}
import io.circe.syntax._
import io.circe.parser
import io.jvm.uuid._

object Scenarios {
  val httpProtocol = http
    .warmUp("http://www.google.com")
    .baseUrl("http://localhost:80")


  def fullReportReceiver = tokenizedScenario("Test")
    .exec(http("Request empty points number")
      .get("/points/info")
      .header("Authorization", "Bearer ${token}")
    ).exec(http("Request empty transaction history")
      .get("/points/transaction/history")
      .header("Authorization", "Bearer ${token}")
      .queryParam("from", getServerTime())
    ).exec(http("Request empty expiring points")
      .get("/points/expiring")
      .header("Authorization", "Bearer ${token}")
    )

  def goldReceiver(amount: Long, amountExpires: Long, howLong: Duration) = tokenizedScenario("Give everyone gold")
    .exec(http("Raise points")
      .post("/points")
      .header("Authorization", "Bearer ${token}")
      .body(mapToJsonBody(Map("amount" -> amount)))
      .asJson
    ).exec(http("Raise expiring points")
      .post("/points")
      .header("Authorization", "Bearer ${token}")
      .body(mapToJsonBody(Map(
        "amount" -> amount.asJson,
        "expires" -> getServerTime(Some(howLong)).asJson)))
      .asJson
    ).exec(http("Request updated points number")
      .get("/points/info")
      .header("Authorization", "Bearer ${token}")
    ).exec(http("Request updated expiring points")
      .get("/points/expiring")
      .header("Authorization", "Bearer ${token}")
    )

  def singleSuccessfulBuy(initialAmout: Long, transactionCost: Long, howLong: Duration) = tokenizedScenario("Single successful buy")
    .exec(http("Request empty points number")
      .get("/points/info")
      .header("Authorization", "Bearer ${token}")
    ).exec(http("Raise points")
      .post("/points")
      .header("Authorization", "Bearer ${token}")
      .body(mapToJsonBody(Map("amount" -> initialAmout)))
      .asJson
    ).exec(http("Start transaction")
      .post("/points/transaction/start")
      .header("Authorization", "Bearer ${token}")
      .body(mapToJsonBody(Map("amount" -> transactionCost)))
      .check(jsonPath("$.transactionId").saveAs("transactionId"))
    ).pause(howLong
    ).exec(
      http("Confirm transaction")
      .post("/points/transaction/confirm")
      .header("Authorization", "Bearer ${token}")
      .body(mapToJsonBody(Map("id" -> "${transactionId}")))
      .asJson
    ).exec(http("Request updated points number")
      .get("/points/info")
      .header("Authorization", "Bearer ${token}")
    )

  def singleChangeOfMind(initialAmout: Long, transactionCost: Long, howLong: Duration) = tokenizedScenario("Single successful buy")
    .exec(http("Request empty points number")
      .get("/points/info")
      .header("Authorization", "Bearer ${token}")
    ).exec(http("Raise points")
      .post("/points")
      .header("Authorization", "Bearer ${token}")
      .body(mapToJsonBody(Map(
        "amount" -> initialAmout.asJson,
        "expires" -> getServerTime(Some(howLong)).asJson)))
      .asJson
      .asJson
    ).exec(http("Start transaction")
      .post("/points/transaction/start")
      .header("Authorization", "Bearer ${token}")
      .body(mapToJsonBody(Map("amount" -> transactionCost)))
      .check(jsonPath("$.transactionId").saveAs("transactionId"))
    ).pause(howLong
    ).exec(
      http("Cancel transaction")
      .post("/points/transaction/cancel")
      .header("Authorization", "Bearer ${token}")
      .body(mapToJsonBody(Map("id" -> "${transactionId}")))
      .asJson
    ).exec(http("Request updated points number")
      .get("/points/info")
      .header("Authorization", "Bearer ${token}")
    )

  def singleSuccessfulExpitingBuy(initialAmout: Long, transactionCost: Long, howLong: Duration) = tokenizedScenario("Single successful buy")
    .exec(http("Request empty points number")
      .get("/points/info")
      .header("Authorization", "Bearer ${token}")
    ).exec(http("Raise points")
      .post("/points")
      .header("Authorization", "Bearer ${token}")
      .body(mapToJsonBody(Map("amount" -> initialAmout)))
      .asJson
    ).exec(http("Start transaction")
      .post("/points/transaction/start")
      .header("Authorization", "Bearer ${token}")
      .body(mapToJsonBody(Map("amount" -> transactionCost)))
      .check(jsonPath("$.transactionId").saveAs("transactionId"))
    ).pause(howLong
    ).exec(
      http("Confirm transaction")
      .post("/points/transaction/confirm")
      .header("Authorization", "Bearer ${token}")
      .body(mapToJsonBody(Map("id" -> "${transactionId}")))
      .asJson
    ).exec(http("Request updated points number")
      .get("/points/info")
      .header("Authorization", "Bearer ${token}")
    )

  def frodoLaBaboulinka(eagerness: Int) = tokenizedScenario("Single successful buy")
    .repeat(eagerness, "i") {
      exec(http("Request frodo points number (La Baboulinka)")
        .get("/points/info")
        .header("Authorization", "Bearer ${token}")
      ).exec(http("Start frodo transaction (La Baboulinka)")
      .post("/points/transaction/start")
      .header("Authorization", "Bearer ${token}")
      .body(mapToJsonBody(Map("amount" -> 1)))
    )}

  private def tokenizedScenario(scenarioName: String) = scenario(scenarioName).exec(s => setToken(s))

  private def getServerTime(addTime: Option[Duration] = None): String = {
    import java.time.{ZonedDateTime, ZoneOffset, ZoneId}
    import java.time.format.DateTimeFormatter
    import java.util.Locale

    var datetime = clock.instant
    addTime.foreach(dur => {
      datetime = datetime.plusMillis(dur.toMillis)
    })
    val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
       .withZone(ZoneId.of("UTC"))

    formatter.format(datetime)
  }

  private def mapToJsonBody[A: Encoder](map: Map[String, A]) = {
    StringBody(map.asJson.toString)
  }

  private implicit val clock: Clock = Clock.systemUTC
  private val settings = appSettings
  private val key = settings.api.auth.signingKey

  private def appSettings: ApplicationSettings = {
    import com.rakuten.market.points.settings.AuthSettings.signingKeyReader
    import pureconfig.{CamelCase, ConfigFieldMapping}
    import pureconfig.generic.auto._
    import pureconfig.generic.ProductHint

    implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))
    pureconfig.loadConfigOrThrow[ApplicationSettings]
  }

  private def setToken(session: Session): Session = {
    import tsec.jwt.JWTClaims
    import tsec.jws.mac.JWTMacImpure
    import tsec.mac.jca.HMACSHA256

    val jti =  Json.fromLong(session.userId)
    val userId = Json.fromString(UUID(0, session.userId).toString)
    val exp = clock.instant.plusSeconds(settings.api.auth.expirationInterval.toSeconds)
    val claim = JWTClaims(customFields = Seq("jti" -> jti,
                                             "userId" -> userId),
                          expiration = Some(exp))
    JWTMacImpure.buildToString[HMACSHA256](claim, key)
      .map(token => session.set("token", token))
      .right
      .get
  }
}
