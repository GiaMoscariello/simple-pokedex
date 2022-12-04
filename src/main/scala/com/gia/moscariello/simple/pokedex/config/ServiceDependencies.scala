package com.gia.moscariello.simple.pokedex.config

import cats.effect.IO
import com.gia.moscariello.simple.pokedex.http.ServerRoutes
import com.gia.moscariello.simple.pokedex.models.{ServerHttpConfig, PokemonApiEndpoints, TranslationEndpoints}
import com.gia.moscariello.simple.pokedex.persistence.{HttpCache, InMemoryCache}
import com.gia.moscariello.simple.pokedex.services.{Engine, PokemonApiClient, TranslationApiClient}
import org.typelevel.log4cats.Logger

case class ServiceDependencies(clientPokemon: PokemonApiClient,
                               translationClient: TranslationApiClient,
                               engine: Engine,
                               serverHttp: ServerRoutes)

object ServiceDependencies {

  def make(implicit logger: Logger[IO]): IO[ServerRoutes] = {
    AppConfig.fromEnv.map {
      config =>
        val cache = new InMemoryCache()
        val pokeClient = pokemonClient(config.client, cache)
        val translationClient = mkTranslationClient(config.client, cache)
        val engine = new Engine(pokeClient, translationClient)

        new ServerRoutes(
          ServerHttpConfig(
            config.server.endpointPokemon,
            config.server.endpointPokemonTranslated,
            config.server.port,
            config.server.host
          ),
          engine)
    }
  }


  private def pokemonClient(config: ClientHttpConfig, cache: HttpCache[String, String])(implicit logger: Logger[IO]): PokemonApiClient = {
    val endpoints = PokemonApiEndpoints(baseUrl = config.pokemonBaseUrl, config.endpointPokemon)
    PokemonApiClient(endpoints, new InMemoryCache())
  }

  private def mkTranslationClient(config: ClientHttpConfig, cache: HttpCache[String, String])(implicit logger: Logger[IO]): TranslationApiClient = {
    val endpoints = TranslationEndpoints(
      baseUrl = config.translationBaseUrl,
      config.enpointYodaTranslation,
      config.endpointShakespeaareTranslation
    )

    TranslationApiClient(endpoints, cache)
  }

}