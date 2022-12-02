import sbt.Keys.parallelExecution

scalaVersion := "2.13.8"
resolvers += "Confluent Repo" at "https://packages.confluent.io/maven"

name := "pokemon-api"
organization := "gia.moscariello"

ThisBuild / version := "0.1.0"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Ywarn-dead-code", "-Ymacro-annotations")

test / parallelExecution := false

libraryDependencies ++=
    Seq(Dependencies.cacheDependencies) ++
    Dependencies.testDependencies ++
    Dependencies.loggingDependencies ++
    Dependencies.circeDependencies ++
    Dependencies.http4sDependencies ++
    Dependencies.sttpDependencies

 assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "io.netty.versions.properties", xs @ _*) => MergeStrategy.discard
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case x                             => MergeStrategy.first
}