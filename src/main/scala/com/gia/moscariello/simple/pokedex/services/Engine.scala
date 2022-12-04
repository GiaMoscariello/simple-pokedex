package com.gia.moscariello.simple.pokedex.services

import cats.data
import cats.effect.IO
import com.gia.moscariello.simple.pokedex.models._
import io.circe.syntax.EncoderOps
import org.http4s.circe.jsonEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.{ParseFailure, Response}

class Engine(private val pokemonClient: PokemonApiClient,
             private val translationClient: TranslationApiClient) {


  def handleRequestFor(queryParameter: data.ValidatedNel[ParseFailure, String], withTranslation: Boolean): IO[Response[IO]] ={
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
