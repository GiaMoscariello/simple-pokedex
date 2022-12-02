import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.github.tomakehurst.wiremock.client.{CountMatchingStrategy, WireMock}
import com.github.tomakehurst.wiremock.client.WireMock._
import io.circe.syntax.EncoderOps
import models.{ApiResponseError, FlavorText, Habitat, Language, PokemonApiEndpoints, PokemonSpecies, PokemonSpeciesApiResponse}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import persistence.{HttpCache, InMemoryCache}
import services.PokemonApiClient


class PokemonApiClientTest extends AnyFlatSpec with BeforeAndAfterEach {
  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  val host = sys.env.getOrElse("WIREMOCK_HOST", "localhost")

  WireMock.configureFor(host, 8080)

  val endpoints = PokemonApiEndpoints(baseUrl = s"http://${host}:8080", pokemonSpecies = "pokemon-species")

  val POKEMON = "mewtwo"
  val WRONG_POKEMON = "not-a-pokemon"
  val POKEMON_SPECIES_URL: String = endpoints.pokemonSpeciesFor(POKEMON)
  val cache: HttpCache[String, String] = new InMemoryCache()

  val stub: PokemonApiClient = services.PokemonApiClient(endpoints, cache)
  lazy val stubWithCache: PokemonApiClient = stub.copy(cache = stubbedCached)

  val pokemonSpeciesApiResponse: PokemonSpeciesApiResponse = PokemonSpeciesApiResponse(200,
    PokemonSpecies("mewtwo",
      isLegendary = true,
      Habitat("rare", "https://pokeapi.co/api/v2/pokemon-habitat/5/"),
      List(
        FlavorText("It was created by a scientist after years of horrificgene splicing and DNA engineering experiments.",
          Language("en", Some("https://pokeapi.co/api/v2/language/9/"))
        )
      )
    )
  )

  val apiResponseError: ApiResponseError = ApiResponseError(Some(s"http://${host}:8080/pokemon-species/not-a-pokemon"),"",404)

  val atLeastOneFor = new CountMatchingStrategy(CountMatchingStrategy.GREATER_THAN_OR_EQUAL, 1)
  val noRequestFor = new CountMatchingStrategy(CountMatchingStrategy.EQUAL_TO, 0)

  private def stubbedCached: HttpCache[String, String] = {
    val stubbedCache: HttpCache[String, String] = new InMemoryCache()

    stubbedCache.put(endpoints.pokemonSpeciesFor(WRONG_POKEMON), apiResponseError.asJson.toString)
    stubbedCache.put(endpoints.pokemonSpeciesFor(POKEMON), pokemonSpeciesApiResponse.pokemon.asJson.toString)

    stubbedCache
  }

  override def beforeEach(): Unit = {
    WireMock.resetAllRequests()
  }

  "calling pokemon-species api for existing pokemon" should "return 200 with the correct pokemon" in {
    val response = stub.pokemonSpecies(POKEMON).unsafeRunSync()

    verify(atLeastOneFor,
      getRequestedFor(urlEqualTo(s"/pokemon-species/${POKEMON}"))
    )

    response match {
      case actual: PokemonSpeciesApiResponse => assert(actual.pokemon.name == POKEMON)
      case error => fail(s"actual response not as excepted ${error.toString}")
    }
  }

  "calling pokemon-species api for cached response" should
    "return 200 and should not call api again" in {
    val response = stubWithCache.pokemonSpecies(POKEMON).unsafeRunSync()

    verify(noRequestFor, getRequestedFor(urlEqualTo(s"/pokemon-species/${POKEMON}")))
    assert(response === pokemonSpeciesApiResponse)
  }

  "calling pokemon-species api for a not existing pokemon" should "return 404" in {
    val response = stub.pokemonSpecies(WRONG_POKEMON).unsafeRunSync()

    verify(atLeastOneFor, getRequestedFor(urlEqualTo(s"/pokemon-species/${WRONG_POKEMON}")))
    assert(response === apiResponseError)
  }

  "calling pokemon-species api for cached response and not existing pokemon" should
    "return 404 and should not call api again" in {
    val response = stubWithCache.pokemonSpecies(WRONG_POKEMON).unsafeRunSync()

    verify(noRequestFor, getRequestedFor(urlEqualTo(s"/pokemon-species/${WRONG_POKEMON}")))
    assert(response === apiResponseError)
  }
}
