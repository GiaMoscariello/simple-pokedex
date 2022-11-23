import sbt._

object Dependencies {
  val DoobieVersion = "1.0.0-RC1"
  val NewTypeVersion = "0.4.4"

  val Log4jVersion = "2.17.1"

  val Http4sVersion = "1.0.0-M21"
  val CirceVersion = "0.15.0-M1"

  val http4sDependencies: Seq[ModuleID] = Seq(
    "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
    "org.http4s" %% "http4s-circe" % Http4sVersion,
    "org.http4s" %% "http4s-dsl" % Http4sVersion,
    "io.github.juliano" % "pokeapi-scala_3" % "0.1.0",

  )

  val circeDependencies: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-core" % CirceVersion,
    "io.circe" %% "circe-generic" % CirceVersion,
    "io.circe" %% "circe-parser" % CirceVersion,
    ("io.circe" %% "circe-generic-extras" % "0.14.3"),
  )

  val loggingDependencies: Seq[ModuleID] = Seq(
    "org.typelevel" %% "log4cats-slf4j" % "2.2.0",
    "ch.qos.logback" % "logback-classic" % "1.2.10",
    "org.slf4j" % "slf4j-simple" % "1.7.36"
  )

  val testDependencies: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % "3.2.11" ,
    "com.dimafeng" %% "testcontainers-scala" % "0.40.0" ,
    "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.40.0"
  )

}
