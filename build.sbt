scalaVersion := "2.13.6"
resolvers += "Confluent Repo" at "https://packages.confluent.io/maven"

name := "pokemon-api"
organization := "gia.moscariello"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Ywarn-dead-code")

crossScalaVersions := Seq("2.13.6", "3.2.1")

libraryDependencies ++=
    Dependencies.testDependencies ++
    Dependencies.loggingDependencies ++
    Dependencies.circeDependencies ++
    Dependencies.http4sDependencies