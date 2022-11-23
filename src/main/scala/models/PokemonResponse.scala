package models

import io.circe.generic.JsonCodec

@JsonCodec
case class PokemonResponse(name: String, description: String, habitat: String, isLegendary: Boolean)
