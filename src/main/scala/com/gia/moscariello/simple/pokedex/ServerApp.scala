package com.gia.moscariello.simple.pokedex

import cats.effect.{ExitCode, IO, IOApp}
import com.gia.moscariello.simple.pokedex.config.ServiceDependencies
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.InetAddress
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration

object ServerApp extends IOApp {
  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    ServiceDependencies
      .make
      .flatMap { server =>
        val routes = server.routes
        BlazeServerBuilder[IO](global)
          .withIdleTimeout(Duration.Inf)
          .bindHttp(server.exposedRoutes.port, server.exposedRoutes.host)
          .withHttpApp(Router("" -> routes).orNotFound)
          .resource
          .use(_ => IO.never)
          .as(ExitCode.Success)
      }
  }
}
