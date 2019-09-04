name := "mini_ichiba_points"

version := "0.1"

scalaVersion := "2.12.9"

scalacOptions ++= Seq(
  "-language:higherKinds",
  "-Ypartial-unification"
)

val Version = new {
  val monix = "3.0.0-RC3"
  val http4s = "0.20.10"
  val circe = "0.11.1"
  val postgresDriver = "42.2.6"
  val quill = "3.4.3"
  val chimney = "0.3.2"
  val scalaLogging = "3.9.2"
  val logback = "1.2.3"
  val flyway = "6.0.1"
}

libraryDependencies ++= Seq(
  "io.monix" %% "monix" % Version.monix,
  
  "org.http4s" %% "http4s-dsl" % Version.http4s,
  "org.http4s" %% "http4s-blaze-server" % Version.http4s,
  "org.http4s" %% "http4s-circe" % Version.http4s,
    
  "io.circe" %% "circe-generic" % Version.circe,
  
  "org.postgresql" % "postgresql" % Version.postgresDriver,
  "io.getquill" %% "quill-jdbc-monix" % Version.quill,

  "com.typesafe.scala-logging" %% "scala-logging" % Version.scalaLogging,
  "ch.qos.logback" % "logback-classic" % Version.logback,

  "org.flywaydb" % "flyway-core" % Version.flyway
)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")