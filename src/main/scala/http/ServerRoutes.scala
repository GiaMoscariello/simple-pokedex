package http

import cats.effect.IO
import io.circe.syntax.EncoderOps
import models.{ApiResponseError, ExposedRoutes, InternalError, Pokemon, PokemonSpeciesApiResponse}
import org.http4s.{HttpRoutes, Response}
import org.http4s.circe.jsonEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import org.typelevel.log4cats.Logger
import org.http4s.server.middleware

class ServerRoutes(private val exposedRoutes: ExposedRoutes,
                   private val pokemonClient: PokemonApiClient,
                   private val translationClient: TranslationApiClient
                  )(implicit logger: Logger[IO]) {

  def routes: HttpRoutes[IO] = {
    val dsl = Http4sDsl[IO]
    import dsl._
    val routes = HttpRoutes.of[IO] {
      case GET -> Root / exposedRoutes.pokemon :? PokemonNameQueryPar(pokemon) => callPokemonSpecies(pokemon)
      case GET -> Root / exposedRoutes.translatedPokemon :? PokemonNameQueryPar(pokemon) => Ok()
    }

    middleware.RequestLogger.httpRoutes(logHeaders = true, logBody = true)(routes)
  }

  private def callPokemonSpecies(pokemon: String): IO[Response[IO]] = {
    val dsl = Http4sDsl[IO]
    import dsl._

    pokemonClient.pokemonSpecies(pokemon)
      .attempt
      .flatMap {
        case Left(err) => logger.error(s"${err.getMessage}") *>
          InternalServerError(InternalError("Internal server error", 500).asJson)
        case Right(response) => response match {
          case re: ApiResponseError => NotFound(re.asJson)
          case pkr: PokemonSpeciesApiResponse => Ok(Pokemon.from(pkr.pokemon).asJson)
        }
      }
  }

  private object PokemonNameQueryPar extends QueryParamDecoderMatcher[String]("pokemon")
}
