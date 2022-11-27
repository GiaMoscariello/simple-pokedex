package http

import cats.effect.IO
import com.github.blemale.scaffeine.Cache
import models.{ApiResponseError, PokemonApiEndpoints, PokemonApiResponse, PokemonSpecies, PokemonSpeciesApiResponse}
import io.circe.parser.decode
import sttp.client3._
import org.typelevel.log4cats.Logger
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

case class PokemonApiClient(endpoint: PokemonApiEndpoints, private val cache: Cache[String, String])(implicit logger: Logger[IO]) {
  def pokemonSpecies(pokemon: String): IO[PokemonApiResponse] = {
    val requestUrl = endpoint.pokemonSpeciesFor(pokemon)
    val request = basicRequest
      .get(uri"${requestUrl}")

    cache.getIfPresent(requestUrl) match {
      case Some(response) => IO.fromEither(handleCachedResult(response, requestUrl))
      case None =>
        AsyncHttpClientCatsBackend
          .resource[IO]()
          .use(backend =>
            request
              .send(backend)
              .flatMap {response =>
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
      pokemon <- IO.fromEither(decode[PokemonSpecies](responseBody))
      _ = cache.put(requestUrl, responseBody)
      pokemonResponse = PokemonSpeciesApiResponse(statusCode, pokemon)
    } yield (pokemonResponse)
  }

  private def handleCachedResult(response: String, requestUrl: String): Either[io.circe.Error, PokemonApiResponse] = {
    decode[PokemonSpecies](response) match {
      case Left(_) => decode[ApiResponseError](response)
      case Right(pokemon) => Right(PokemonSpeciesApiResponse(
        statusCode = 200, pokemon = pokemon
      ))
    }
  }
}
