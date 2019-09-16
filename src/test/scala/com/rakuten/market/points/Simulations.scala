package com.rakuten.market.points


import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._


class BasicSimulation extends Simulation {
  val userNumber = 10
  val startTime = 10.seconds

  setUp(
    Scenarios.fullReportReceiver
      .inject(rampUsers(userNumber) during (startTime))
      .protocols(Scenarios.httpProtocol)
  )
}

class SimulationWithWriters extends Simulation {
  val userNumberReader = 9
  val userNumberWriter = 1
  val startTime = 10.seconds
  val waitTime = 5.seconds

  setUp(
    Scenarios.fullReportReceiver
      .inject(rampUsers(userNumberReader) during (startTime))
      .protocols(Scenarios.httpProtocol),
    Scenarios.singleSuccessfulBuy(50, 20, waitTime)
      .inject(rampUsers(userNumberWriter) during (startTime))
      .protocols(Scenarios.httpProtocol),
  )
}

class SimulationWithWritersAndCancels extends Simulation {
  val userNumberReader = 9
  val userNumberWriterConfirm = 5
  val userNumberWriterCancel = 5
  val startTime = 10.seconds
  val waitTime = 5.seconds

  setUp(
    Scenarios.fullReportReceiver
      .inject(rampUsers(userNumberReader) during (startTime))
      .protocols(Scenarios.httpProtocol),
    Scenarios.singleSuccessfulBuy(50, 20, waitTime)
      .inject(rampUsers(userNumberWriterConfirm) during (startTime))
      .protocols(Scenarios.httpProtocol),
    Scenarios.singleChangeOfMind(50, 20, waitTime)
      .inject(rampUsers(userNumberWriterCancel) during (startTime))
      .protocols(Scenarios.httpProtocol),
  )
}

class SimulationWithExpiringWritersAndCancels extends Simulation {
  val userNumberReader = 9
  val userNumberWriterConfirm = 5
  val userNumberWriterCancel = 5
  val startTime = 10.seconds
  val waitTime = 5.seconds

  setUp(
    Scenarios.fullReportReceiver
      .inject(rampUsers(userNumberReader) during (startTime))
      .protocols(Scenarios.httpProtocol),
    Scenarios.singleSuccessfulExpitingBuy(50, 20, waitTime)
      .inject(rampUsers(userNumberWriterConfirm) during (startTime))
      .protocols(Scenarios.httpProtocol),
    Scenarios.singleChangeOfMind(50, 20, waitTime)
      .inject(rampUsers(userNumberWriterCancel) during (startTime))
      .protocols(Scenarios.httpProtocol),
  )
}

class SimulationWithExpiringWritersAndCancelsWithFrodo extends Simulation {
  val userNumberReader = 9
  val userNumberWriterConfirm = 5
  val userNumberWriterCancel = 5
  val userNumberFrodo = 2
  val frodoEagerness = 1
  val startTime = 10.seconds
  val waitTime = 5.seconds

  setUp(
    Scenarios.fullReportReceiver
      .inject(rampUsers(userNumberReader) during (startTime))
      .protocols(Scenarios.httpProtocol),
    Scenarios.singleSuccessfulExpitingBuy(50, 20, waitTime)
      .inject(rampUsers(userNumberWriterConfirm) during (startTime))
      .protocols(Scenarios.httpProtocol),
    Scenarios.singleChangeOfMind(50, 20, waitTime)
      .inject(rampUsers(userNumberWriterCancel) during (startTime))
      .protocols(Scenarios.httpProtocol),
    Scenarios.frodoLaBaboulinka(frodoEagerness)
      .inject(rampUsers(userNumberFrodo) during (startTime))
      .protocols(Scenarios.httpProtocol),
  )
}