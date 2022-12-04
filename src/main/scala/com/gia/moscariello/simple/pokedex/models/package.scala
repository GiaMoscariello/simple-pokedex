package com.gia.moscariello.simple.pokedex

import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec}

package object models {
  implicit lazy val config: Configuration = Configuration.default.withDefaults.withSnakeCaseMemberNames

  trait ApiResponse

  trait PokemonApiResponse

  trait HttpError

  trait Endpoints

  trait TranslationResponse


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

  case class ServerHttpConfig(pokemon: String, translatedPokemon: String, port: Int, host: String)

  case class PokemonApiEndpoints(baseUrl: String, pokemonSpecies: String) extends Endpoints {
    val pokemonSpeciesFor: String => String =
      (pokemon: String) => s"${baseUrl}/${pokemonSpecies}/${pokemon}"
  }

  case class TranslationEndpoints(baseUrl: String, yoda: String, shakespeare: String) extends Endpoints {
    val yodaEndpoint = s"${baseUrl}/${yoda}"
    val shakespeareEndpoint = s"${baseUrl}/${shakespeare}"
  }

  @ConfiguredJsonCodec case class TranslationRequest(text: String)

  @ConfiguredJsonCodec case class TranslationResponseSuccess(contents: Contents) extends TranslationResponse

  @ConfiguredJsonCodec case class Contents(translated: String, text: String, translation: String)

  @ConfiguredJsonCodec case class TranslationResponseError(error: ErrorTranslation)
    extends TranslationResponse
      with HttpError

  @ConfiguredJsonCodec case class ErrorTranslation(code: Int, message: String)


  object Pokemon {
    def from(species: PokemonSpecies): Pokemon = {
      Pokemon(
        name = species.name,
        isLegendary = species.isLegendary,
        habitat = species.habitat.name,
        description = species.flavorTextEntries
          .map(cleanUpFlavorText)
          .find(_.language.name == "en")
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

  /** *
   * from PokeApi docs:
   * "The localized flavor text for an API resource in a specific language. Note that this text is left unprocessed as it is found in game files.
   * This means that it contains special characters that one might want to replace with their visible decodable version.
   * Please check out this issue to find out more -> https://github.com/veekun/pokedex/issues/218#issuecomment-339841781"
   *
   */

  private def cleanUpFlavorText(flavorText: FlavorText): FlavorText = {
    val textCleaned = flavorText.flavorText
      .replace("\f", "\n")
      .replace("\u00ad\n", "")
      .replace("\u00ad", "")
      .replace(" -\n", " - ")
      .replace("-\n", "-")
      .replace("\n", " ")

    FlavorText(textCleaned, flavorText.language)
  }
}

