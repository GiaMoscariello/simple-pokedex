package config

import cats.effect.IO
import http.ServerRoutes
import services.{Engine, PokemonApiClient, TranslationApiClient}

case class Dependencies(clientPokemon: PokemonApiClient,
                        translationClient: TranslationApiClient,
                        engine: Engine,
                        serverHttp: ServerRoutes)

object Dependencies {

}