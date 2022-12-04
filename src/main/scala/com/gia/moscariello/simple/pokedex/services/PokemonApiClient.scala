package com.gia.moscariello.simple.pokedex.services

import cats.effect.IO
import com.gia.moscariello.simple.pokedex.models._
import com.gia.moscariello.simple.pokedex.persistence.HttpCache
import org.typelevel.log4cats.Logger
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.{UriContext, basicRequest}
import io.circe.parser.decode
import io.circe.syntax.EncoderOps


case class PokemonApiClient(endpoint: PokemonApiEndpoints, private val cache: HttpCache[String, String])(implicit logger: Logger[IO]) {
  def pokemonSpecies(pokemon: String): IO[PokemonApiResponse] = {
    val requestUrl = endpoint.pokemonSpeciesFor(pokemon)
    val request = basicRequest
      .get(uri"${requestUrl}")

    cache.get(requestUrl) flatMap {
      case Some(response) => IO.fromEither(handleCachedResult(response, requestUrl))
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

  private def handleErrorResponse(errorBody: String, requestUrl: String, statusCode: Int): IO[PokemonApiResponse] =
    IO(ApiResponseError(Some(requestUrl), errorBody, statusCode))

  private def handleSuccessfulResponse(responseBody: String, requestUrl: String, statusCode: Int): IO[PokemonApiResponse] = {
    for {
      pokemon <- IO.fromEither(decode[PokemonSpecies](responseBody))
      _ <- cache.put(requestUrl, responseBody)
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
