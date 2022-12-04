package com.gia.moscariello.simple.pokedex.http

import cats.data
import cats.effect.IO
import com.gia.moscariello.simple.pokedex.models.{InternalError, ServerHttpConfig}
import com.gia.moscariello.simple.pokedex.services.Engine
import org.http4s.{HttpRoutes, ParseFailure}
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.ValidatingQueryParamDecoderMatcher
import org.http4s.server.middleware
import org.typelevel.log4cats.Logger

class ServerRoutes(val exposedRoutes: ServerHttpConfig, private val engine: Engine)(implicit logger: Logger[IO]) {

  def routes: HttpRoutes[IO] = {
    val dsl = Http4sDsl[IO]
    import dsl._
    val routes = HttpRoutes.of[IO] {
      case GET -> Root / exposedRoutes.pokemon :? PokemonNameQueryPar(pokemon) => mkResponseFor(pokemon, withTranslation = false)
      case GET -> Root / exposedRoutes.translatedPokemon :? PokemonNameQueryPar(pokemon) => mkResponseFor(pokemon, withTranslation = true)
      case request => logger.error("Routes not found") *> NotFound(s"Routes not found $request")
    }

    middleware.RequestLogger.httpRoutes(logHeaders = true, logBody = true)(routes)
    middleware.ResponseLogger.httpRoutes(logHeaders = true, logBody = true)(routes)
  }

  private def mkResponseFor(pokemon: data.ValidatedNel[ParseFailure, String], withTranslation: Boolean) = {
    val dsl = Http4sDsl[IO]
    import dsl._

    engine.handleRequestFor(pokemon, withTranslation)
      .attempt.flatMap {
      case Left(e) => InternalServerError(InternalError(s"Internal server error, please retry in a few moments. ${e.getMessage}" , 500))
      case Right(s) => IO.pure(s)
    }
  }

  private object PokemonNameQueryPar extends ValidatingQueryParamDecoderMatcher[String]("name")
}
