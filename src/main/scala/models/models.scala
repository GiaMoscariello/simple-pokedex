package models

import http.Endpoints

object models {
  case class ExposedRoutes(pokemon: String, translatedPokemon: String)

  case class PokemonApiEndpoints(baseUrl: String, pokemonSpecies: String) extends Endpoints {
    val pokemonSpeciesFor: String => String =
      (pokemon: String) => s"${baseUrl}/${pokemonSpecies}/${pokemon}"
  }
}