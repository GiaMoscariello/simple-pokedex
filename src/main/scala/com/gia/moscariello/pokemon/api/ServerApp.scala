package com.gia.moscariello.pokemon.api

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.blaze.server.BlazeServerBuilder
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.InetAddress
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration

object ServerApp extends IOApp {
  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    BlazeServerBuilder[IO](global)
      .withResponseHeaderTimeout(2.minutes)
      .withIdleTimeout(Duration.Inf)
      .bindHttp(9090, InetAddress.getLocalHost.getHostName)
      .resource
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }
}
