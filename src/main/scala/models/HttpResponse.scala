package models

import io.circe.generic.JsonCodec
import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec}

trait PokemonApiResponse

trait HttpError

object HttpModels {

  implicit lazy val config: Configuration = Configuration.default.withDefaults.withSnakeCaseMemberNames

  @ConfiguredJsonCodec case class PokemonSpeciesApiResponse(statusCode: Int, pokemon: PokemonSpecies) extends PokemonApiResponse

  @ConfiguredJsonCodec case class PokemonSpecies(name: String,
                                                 isLegendary: Boolean,
                                                 habitat: Habitat,
                                                 flavorTextEntries: List[FlavorText])

  @ConfiguredJsonCodec case class Pokemon(name: String, description: String, habitat: String, isLegendary: Boolean)

  @ConfiguredJsonCodec case class Habitat(name: String, url: String)

  @ConfiguredJsonCodec case class ApiResponseError(endpoint: Option[String], error: String, errorCode: Int)
    extends PokemonApiResponse
      with HttpError

  @ConfiguredJsonCodec case class InternalError(error: String, errorCode: Int) extends HttpError

  @ConfiguredJsonCodec case class FlavorText(flavorText: String, language: Language)

  @ConfiguredJsonCodec case class Language(name: String, url: Option[String])

  object Pokemon {
    def from(species: PokemonSpecies): Pokemon = {
      Pokemon(
        name = species.name,
        isLegendary = species.isLegendary,
        habitat = species.habitat.name,
        description = species.flavorTextEntries.findLast(_.language.name == "en")
          .fold("no description available")(_.flavorText)
      )
    }

    def fromTranslatedText(species: PokemonSpecies, translatedText: String): Pokemon = {
      Pokemon(
        name = species.name,
        isLegendary = species.isLegendary,
        habitat = species.habitat.name,
        description = translatedText
      )
    }
  }
}