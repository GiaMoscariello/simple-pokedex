package http

import cats.data
import cats.data.NonEmptyList
import cats.effect.IO
import io.circe.syntax.EncoderOps
import models.{ApiResponseError, ExposedRoutes, HttpError, InternalError, Pokemon, PokemonSpeciesApiResponse, TranslationRequest, TranslationResponse, TranslationResponseError, TranslationResponseSuccess}
import org.http4s.{HttpRoutes, ParseFailure, Response}
import org.http4s.circe.jsonEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.{QueryParamDecoderMatcher, ValidatingQueryParamDecoderMatcher}
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
      case GET -> Root / exposedRoutes.pokemon :? PokemonNameQueryPar(pokemon) => handleRequestFor(pokemon, withTranslation = false)
      case GET -> Root / exposedRoutes.translatedPokemon :? PokemonNameQueryPar(pokemon) => handleRequestFor(pokemon, withTranslation = true)
      case request => logger.error("Routes not found") *> NotFound(s"Routes not found ${request}")
    }

    middleware.RequestLogger.httpRoutes(logHeaders = true, logBody = true)(routes)
    middleware.ResponseLogger.httpRoutes(logHeaders = true, logBody = true)(routes)
  }

  private object PokemonNameQueryPar extends ValidatingQueryParamDecoderMatcher[String]("name")

  private def handleRequestFor(queryParameter: data.ValidatedNel[ParseFailure, String], withTranslation: Boolean): IO[Response[IO]] ={
    val dsl = Http4sDsl[IO]
    import dsl._

    queryParameter.fold(
      error => BadRequest(s"Wrong query parameter type: ${error.head.toString}"),
      cp    =>   getPokemonSpecies(cp).flatMap {
        case Left(err) => createErrorResponse(err)
        case Right(pokemon) => createPokemonResponseFor(pokemon, withTranslation)
      }
    )
  }

  private def getPokemonSpecies(pokemon: String): IO[Either[HttpError, Pokemon]] =
    pokemonClient
      .pokemonSpecies(pokemon)
      .attempt
      .flatMap {
        case Left(_) => IO(Left(InternalError("Internal server error", 500)))
        case Right(response) => response match {
          case re: ApiResponseError => IO(Left(re))
          case pkr: PokemonSpeciesApiResponse => IO(Right(Pokemon.from(pkr.pokemon)))
        }
      }

  private def createPokemonResponseFor(pokemon: Pokemon, needTranslation: Boolean): IO[Response[IO]] = {
    val dsl = Http4sDsl[IO]
    import dsl._
    if (!needTranslation) Ok(pokemon.asJson)
    else translatePokemonDescription(pokemon).flatMap(pk => Ok(pk.asJson))
  }

  private def translatePokemonDescription(pokemon: Pokemon): IO[Pokemon] = {
    val bodyRequest = TranslationRequest(pokemon.description)
    if (yodaTranslationCase(pokemon)) {
      createTranslatedPokemonFrom(translationClient.yoda(bodyRequest), pokemon)
    } else createTranslatedPokemonFrom(translationClient.shakespeare(bodyRequest), pokemon)
  }

  private def createErrorResponse(httpError: HttpError): IO[Response[IO]] = {
    val dsl = Http4sDsl[IO]
    import dsl._

    httpError match {
      case re: ApiResponseError => NotFound(re.asJson)
      case ie: InternalError => InternalServerError(ie.asJson)
    }
  }

  private def createTranslatedPokemonFrom(response: IO[TranslationResponse], pokemon: Pokemon): IO[Pokemon] = {
    response.map {
      case _: TranslationResponseError => pokemon
      case succ: TranslationResponseSuccess => pokemon.copy(description = succ.contents.translated)
    }
  }

  private val yodaTranslationCase: (Pokemon => Boolean) =
    (pokemon: Pokemon) => pokemon.habitat == "cave" || pokemon.isLegendary

}
