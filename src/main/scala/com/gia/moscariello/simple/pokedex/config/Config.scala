package com.gia.moscariello.simple.pokedex.config

import cats.effect.IO
import cats.implicits._

case class ServerHttpConfig(port: Int, endpointPokemon: String, endpointPokemonTranslated: String, host: String)

case class ClientHttpConfig(pokemonBaseUrl: String,
                            translationBaseUrl: String,
                            endpointPokemon: String,
                            endpointPokemonTranslated: String,
                            enpointYodaTranslation: String,
                            endpointShakespeaareTranslation: String
                           )

case class AppConfig(server: ServerHttpConfig, client: ClientHttpConfig)

object AppConfig {
  def fromEnv: IO[AppConfig] = {
    (ServerHttpConfig.fromEnv, ClientHttpConfig.fromEnv).mapN((server, client) => AppConfig(
      server, client)
    )
  }
}


object ServerHttpConfig {
  def fromEnv: IO[ServerHttpConfig] = {
    IO.delay(
      ServerHttpConfig(
        port = sys.env("SERVER_PORT").toInt,
        endpointPokemon = sys.env("SERVER_ENDPOINT_POKEMON"),
        endpointPokemonTranslated = sys.env("SERVER_ENDPOINT_POKEMON_TRANSLATED"),
        host = sys.env("SERVER_HOST")
      )
    )
  }
}

object ClientHttpConfig {
  def fromEnv: IO[ClientHttpConfig] = {
    IO.delay(
      ClientHttpConfig(
        pokemonBaseUrl = sys.env("POKEMON_BASE_URL"),
        translationBaseUrl = sys.env("TRANSLATION_BASE_URL"),
        endpointPokemon = sys.env("CLIENT_ENDPOINT_POKEMON"),
        endpointPokemonTranslated = sys.env("SERVER_ENDPOINT_POKEMON_TRANSLATED"),
        enpointYodaTranslation = sys.env("ENDPOINT_YODA_TRANSLATION"),
        endpointShakespeaareTranslation = sys.env("ENDPOINT_SHAKESPEARE_TRANSLATION")
      )
    )
  }
}