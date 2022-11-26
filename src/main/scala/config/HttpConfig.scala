package config

import cats.effect.IO

case class ServerHttpConfig(port: Int)

case class ClientHttpConfig(endpointPokemon: String,
                            endpointPokemonTranslated: String,
                            enpointYodaTranslation: String,
                            endpointShakespeaareTranslation: String
                           )


object ServerHttpConfig {
  def fromEnv: IO[ServerHttpConfig] = {
    IO.delay(
      ServerHttpConfig(sys.env("SERVER_PORT").toInt)
    )
  }
}

object ClientHttpConfig {
  def fromEnv: IO[ClientHttpConfig] = {
    IO.delay(
      ClientHttpConfig(
        endpointPokemon = sys.env("ENDPOINT_POKEMON"),
        endpointPokemonTranslated = sys.env("ENDPOINT_POKEMON_TRANSLATED"),
        enpointYodaTranslation = sys.env("ENDPOINT_YODA_TRANSLATION"),
        endpointShakespeaareTranslation = sys.env("ENDPOINT_SHAKESPEARE_TRANSLATION")
      )
    )
  }
}