scalaVersion := "2.13.8"
resolvers += "Confluent Repo" at "https://packages.confluent.io/maven"

name := "pokemon-api"
organization := "gia.moscariello"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Ywarn-dead-code", "-Ymacro-annotations")

libraryDependencies ++=
    Seq(Dependencies.cacheDependencies) ++
    Dependencies.testDependencies ++
    Dependencies.loggingDependencies ++
    Dependencies.circeDependencies ++
    Dependencies.http4sDependencies ++
    Dependencies.sttpDependencies