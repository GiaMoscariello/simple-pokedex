import sbt._


object Dependencies {

  val scalaVersion = "2.13.8"
  val Log4jVersion = "2.17.2"
  val CirceVersion = "0.14.2"
  val SttpVersion = "3.6.2"
  val TestContainersVersion = "0.40.8"
  val MockServerJavaVersion = "5.13.2"
  val CatsEffectVersion = "3.3.12"

  val Http4sVersion = "0.23.0"

  val catsDependencies: ModuleID = "org.typelevel" %% "cats-effect" % CatsEffectVersion
  val scalaXmlDeps = "org.scala-lang.modules" %% "scala-xml" % "2.1.0"

  val circeDependencies: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-core" % CirceVersion,
    "io.circe" %% "circe-generic" % CirceVersion,
    "io.circe" %% "circe-parser" % CirceVersion,
    "io.circe" %% "circe-generic-extras" % CirceVersion,
  )

  val http4sDependencies: Seq[ModuleID] = Seq(
    "org.http4s" %% "http4s-blaze-server",
    "org.http4s" %% "http4s-circe",
    "org.http4s" %% "http4s-dsl"
  ).map(_ % Http4sVersion)


  val testDependencies: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % "3.2.12" % Test,
    "com.github.tomakehurst" % "wiremock-jre8" % "2.35.0" % Test
  )

  val loggingDependencies: Seq[ModuleID] = Seq(
    "org.typelevel" %% "log4cats-slf4j" % "2.3.0",
    "ch.qos.logback" % "logback-classic" % "1.2.11"
  )

  val sttpDependencies: Seq[ModuleID] = Seq(
    "com.softwaremill.sttp.client3" %% "core",
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats",
  ).map(_ % SttpVersion)

  val cacheDependencies =
    "com.github.blemale" %% "scaffeine" % "5.1.2"
}
