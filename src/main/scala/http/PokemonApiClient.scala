package http

import cats.effect.IO
import com.github.blemale.scaffeine.{Cache, Scaffeine}
import models.PokemonApiResponse
import models.models._
import io.circe.parser.decode
import models.HttpModels.{ApiResponseError, PokemonSpecies, PokemonSpeciesApiResponse}
import sttp.client3._
import org.typelevel.log4cats.Logger
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

case class PokemonApiClient(endpoint: PokemonApiEndpoints, cache: Cache[String, String], backend: SttpBackend[IO, String])(implicit logger: Logger[IO]) {
  def pokemonSpecies(pokemon: String): IO[PokemonApiResponse] = {
    val requestUrl = endpoint.pokemonSpeciesFor(pokemon)
    val request = basicRequest.get(uri"${requestUrl}")

    cache.getIfPresent(requestUrl) match {
      case Some(response) => IO.fromEither(decode[PokemonSpecies](response))
        .map(pm => PokemonSpeciesApiResponse(
          statusCode = 200, pokemon = pm
        ))
      case None =>
        request
          .send(backend)
          .flatMap { response =>
            val statusCode = response.code.code
            response.body.fold[IO[PokemonApiResponse]](
              error => handleErrorResponse(error, requestUrl, statusCode),
              success => handleSuccessfulResponse(success, requestUrl, statusCode)
            )
          }
    }
  }

  def pokemonSpecies2(pokemon: String): IO[PokemonApiResponse] = {
    val requestUrl = endpoint.pokemonSpeciesFor(pokemon)
    val request = basicRequest
      .get(uri"${requestUrl}")

    cache.getIfPresent(requestUrl) match {
      case Some(response) => IO.fromEither(decode[PokemonSpecies](response))
        .map(pm => PokemonSpeciesApiResponse(
          statusCode = 200, pokemon = pm
        ))
      case None =>
        AsyncHttpClientCatsBackend
          .resource[IO]()
          .use(backend =>
            request
              .send(backend)
              .flatMap { response =>
                val statusCode = response.code.code
                response.body.fold[IO[PokemonApiResponse]](
                  error => handleErrorResponse(error, requestUrl, statusCode),
                  success => handleSuccessfulResponse(success, requestUrl, statusCode)
                )
              })
    }
  }

  private def handleErrorResponse(errorBody: String, requestUrl: String, statusCode: Int): IO[PokemonApiResponse] = {
    cache.put(requestUrl, errorBody)
    IO(ApiResponseError(Some(requestUrl), errorBody, statusCode))
  }

  private def handleSuccessfulResponse(responseBody: String, requestUrl: String, statusCode: Int): IO[PokemonApiResponse] = {
    for {
      _ <- logger.info(s"received response: ${responseBody}")
      pokemon <- IO.fromEither(decode[PokemonSpecies](responseBody))
      _ = cache.put(requestUrl, responseBody)
      pokemonResponse = PokemonSpeciesApiResponse(statusCode, pokemon)
    } yield (pokemonResponse)
  }
}
