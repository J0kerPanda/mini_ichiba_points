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
}

libraryDependencies ++= Seq(
  "io.monix" %% "monix" % Version.monix,
  
  "org.http4s" %% "http4s-dsl" % Version.http4s,
  "org.http4s" %% "http4s-blaze-server" % Version.http4s,
  
  "org.postgresql" % "postgresql" % "42.2.6",
  "io.getquill" %% "quill-jdbc-monix" % "3.4.3"
)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")