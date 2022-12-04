import com.gia.moscariello.simple.pokedex.models._
import org.scalatest.flatspec.AnyFlatSpec

class PokemonParsingTest extends AnyFlatSpec {

  val pokemonSpecies = PokemonSpecies("mewtwo",
    isLegendary = true,
    Habitat("rare", "https://pokeapi.co/api/v2/pokemon-habitat/5/"),
    List(
      FlavorText("It was created by a scientist after years of horrificgene splicing and DNA engineering experiments.",
        Language("en", Some("https://pokeapi.co/api/v2/language/9/"))
      )
    )
  )

  val flavorTexts = List(
    FlavorText("It was created by a scientist after years of horrificgene splicing and DNA engineering experiments.",
      Language("en", None)
    ),
    FlavorText("Son ADN est presque le même que celui de\\nMew, mais sa taille et son caractère sont très\\ndifférents.",
      Language("fr", None)),
    FlavorText("Mewtu und Mew weisen sehr ähnliche Gene auf,\\ndoch hinsichtlich ihres Charakters und ihrer\\nGröße unterscheiden sich die beiden erheblich.",
      Language("de", None)),
    FlavorText("Su ADN es casi el mismo que el de Mew.\\nSin embargo, su tamaño y carácter son muy\\ndiferentes.",
      Language("es", None))
  )

  val pokemonExcepted = Pokemon("mewtwo",
    "It was created by a scientist after years of horrific gene splicing and DNA engineering experiments.",
    "rare",
    true)

  "pokemon response" should "be clean up from control chars" in {
    assert(Pokemon.from(pokemonSpecies) === pokemonExcepted)
  }

  "pokemon response" should "contains only english flavor text" in {
    val pokemonSpeciesResponse = PokemonSpecies(
      "mewtwo",
      isLegendary = true,
      Habitat("rare", "https://pokeapi.co/api/v2/pokemon-habitat/5/"),
      flavorTexts)

    assert(Pokemon.from(pokemonSpeciesResponse) === pokemonExcepted)
  }

  "pokemon response for translation api" should "correctly use translated text" in {
    val translated= "Created by a scientist after years of horrific gene splicing and dna engineering experiments,  it was."
    assert(Pokemon.fromTranslatedText(pokemonSpecies, translated).description === translated)
  }
}
