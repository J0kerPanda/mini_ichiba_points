package com.rakuten.market.points

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

// class BasicItSimulation extends Simulation {
//   // setUp(Scenarios.testScenario.inject(atOnceUsers(1)).protocols(Scenarios.httpProtocol))
// //   setUp(Scenarios.singleSuccessfulExpitingBuy(50, 20, 5.seconds).inject(atOnceUsers(1)).protocols(Scenarios.httpProtocol))
//   setUp(Scenarios.frodoLaBaboulinka(100).inject(atOnceUsers(1)).protocols(Scenarios.httpProtocol))
// }

class BasicSimulation extends Simulation {
  val userNumber = 10000
  val startTime = 10.seconds

  setUp(
    Scenarios.fullReportReceiver
      .inject(rampUsers(userNumber) during (startTime))
      .protocols(Scenarios.httpProtocol)
  )
}

class SimulationWithWriters extends Simulation {
  val userNumberReader = 9000
  val userNumberWriter = 1000
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
  val userNumberReader = 9000
  val userNumberWriterConfirm = 500
  val userNumberWriterCancel = 500
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
  val userNumberReader = 9000
  val userNumberWriterConfirm = 500
  val userNumberWriterCancel = 500
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
  val userNumberReader = 9000
  val userNumberWriterConfirm = 500
  val userNumberWriterCancel = 500
  val userNumberFrodo = 20
  val frodoEagerness = 100
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
