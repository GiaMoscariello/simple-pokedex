package http

import cats.effect.IO
import models.ExposedRoutes
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.ValidatingQueryParamDecoderMatcher
import org.http4s.server.middleware
import org.typelevel.log4cats.Logger
import services.Engine

class ServerRoutes(private val exposedRoutes: ExposedRoutes, private val engine: Engine)(implicit logger: Logger[IO]) {

  def routes: HttpRoutes[IO] = {
    val dsl = Http4sDsl[IO]
    import dsl._
    val routes = HttpRoutes.of[IO] {
      case GET -> Root / exposedRoutes.pokemon :? PokemonNameQueryPar(pokemon) => engine.handleRequestFor(pokemon, withTranslation = false)
      case GET -> Root / exposedRoutes.translatedPokemon :? PokemonNameQueryPar(pokemon) => engine.handleRequestFor(pokemon, withTranslation = true)
      case request => logger.error("Routes not found") *> NotFound(s"Routes not found ${request}")
    }

    middleware.RequestLogger.httpRoutes(logHeaders = true, logBody = true)(routes)
    middleware.ResponseLogger.httpRoutes(logHeaders = true, logBody = true)(routes)
  }

  private object PokemonNameQueryPar extends ValidatingQueryParamDecoderMatcher[String]("name")
}
