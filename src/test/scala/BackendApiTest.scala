import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.github.blemale.scaffeine.{Cache, Scaffeine}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import http.PokemonApiClient
import io.circe.syntax.EncoderOps
import models.HttpModels.{ApiResponseError, FlavorText, Habitat, Language, PokemonSpecies, PokemonSpeciesApiResponse}
import models.PokemonApiResponse
import models.models.PokemonApiEndpoints
import org.mockserver.client.MockServerClient
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest.{request => mockClientRequest}
import org.mockserver.model.HttpResponse.{response => mockClientResponse}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import sttp.client3.armeria.cats.ArmeriaCatsBackend
import sttp.client3.{Response, UriContext, basicRequest}
import sttp.client3.impl.cats.implicits.asyncMonadError
import sttp.client3.testing.{RecordingSttpBackend, SttpBackendStub}
import sttp.model.StatusCode
import sttp.monad.MonadAsyncError

class BackendApiTest extends AnyFlatSpec with BeforeAndAfterEach {
  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  val endpoint = PokemonApiEndpoints(baseUrl = "http://pokemon/api/v2", pokemonSpecies = "pokemon-species")
  val stubBackend = SttpBackendStub[IO, String](implicitly[MonadAsyncError[IO]])

  val port = 8080
  val host = "localhost"
  val endpoints = PokemonApiEndpoints(baseUrl = "http://localhost:8080", pokemonSpecies = "pokemon-species")
  val wireMockServer = new WireMockServer(wireMockConfig().port(port))

  val responseFilePath = "src/test/resources/mewtwo-correct-response.json"
  val MEW_TWO = "mewtwo"
  val WRONG_POKEMON = "not-a-pokemon"
  val mewtwoSpeciesUrl: String = endpoint.pokemonSpeciesFor(MEW_TWO)

  val cache: Cache[String, String] = Scaffeine()
    .recordStats()
    .build[String, String]()

  override def beforeEach() = {
    wireMockServer.resetAll()
  }


  override def afterEach() = {
    wireMockServer.stop()
  }

  val mewTwoPokemonSpeciesResponse: PokemonSpeciesApiResponse = PokemonSpeciesApiResponse(200,
    PokemonSpecies("mewtwo",
      true,
      Habitat("rare", "https://pokeapi.co/api/v2/pokemon-habitat/5/"),
      List(
        FlavorText("It was created by a scientist after years of horrificgene splicing and DNA engineering experiments.",
          Language("en", Some("https://pokeapi.co/api/v2/language/9/"))
        )
      )
    )
  )

  private val checkCacheHit: (PokemonApiClient => Long) = (client: PokemonApiClient) =>
    client.cache.stats().hitCount()

  private val checkApiRequest: (RecordingSttpBackend[IO, String] => Int) = (client: RecordingSttpBackend[IO, String]) =>
    client.allInteractions.size


  "calling pokemon-species with existing pokemon without cache" should "return status 200" in {
    val path = endpoint.pokemonSpeciesFor(MEW_TWO)
    val request = basicRequest.get(uri"${path}")

    val stub = new RecordingSttpBackend(stubBackend
      .whenRequestMatches(_ == request)
      .thenRespond(mewTwoPokemonSpeciesResponse.pokemon.asJson.toString))

    val client = PokemonApiClient(endpoint, cache, stub)

    val pokemonResponse: PokemonApiResponse = client.pokemonSpecies(MEW_TWO).unsafeRunSync()

    pokemonResponse match {
      case psr: PokemonSpeciesApiResponse => assert(
        psr.statusCode == 200 && checkCacheHit(client) == 0 && checkApiRequest(stub) == 1
      )
      case _ => fail("response failed")
    }
  }

  "calling pokemon-species with a wrong pokemon without cache" should "return an error response with status 404" in {
    val path = endpoint.pokemonSpeciesFor(WRONG_POKEMON)
    val request = basicRequest.get(uri"${path}")

    val stub = new RecordingSttpBackend(stubBackend
      .whenRequestMatches(_ == request)
      .thenRespond(Response("Not found", StatusCode.NotFound)))

    val client = PokemonApiClient(endpoint, cache, stub)

    val pokemonResponse: PokemonApiResponse = client.pokemonSpecies(WRONG_POKEMON).unsafeRunSync()

    pokemonResponse match {
      case error: ApiResponseError => assert(
        error.errorCode == 404 && (checkCacheHit(client) == 0 && checkApiRequest(stub) == 1))
      case _ => fail("response failed")
    }
  }

  "calling pokemon-species with cached request" should "not call api second time" in {
    val path = endpoint.pokemonSpeciesFor(MEW_TWO)
    val request = basicRequest.get(uri"${path}")
    val response = mewTwoPokemonSpeciesResponse.pokemon.asJson.toString

    val stub: RecordingSttpBackend[IO, String] = new RecordingSttpBackend(stubBackend
      .whenRequestMatches(_ == request)
      .thenRespond(response))

    val client = PokemonApiClient(endpoint, cache, stub)

    val response1 = client.pokemonSpecies(MEW_TWO).unsafeRunSync()
    val response2 = client.pokemonSpecies(MEW_TWO).unsafeRunSync()

    response2 match {
      case psr: PokemonSpeciesApiResponse => assert(
        psr.statusCode == 200 && (checkCacheHit(client) == 1) && (checkApiRequest(stub) == 1))
      case _ => fail("response failed")
    }
  }

  "calling pokemon-species with cached request mockserver" should "not call api second time" in {
    val Port = 1080
    val Host = "localhost"
    val mockServer = new MockServerClient("localhost", Port)
    val endpoint = PokemonApiEndpoints(baseUrl = "http://localhost:1080", pokemonSpecies = "pokemon-species")
    val response = mewTwoPokemonSpeciesResponse.pokemon.asJson.toString

    mockServer.when(
      mockClientRequest()
        .withMethod("GET")
        .withPath("/pokemon-species/mewtwo")
    ).respond(
      mockClientResponse()
        .withBody(response)
        .withStatusCode(200))

    val stub: RecordingSttpBackend[IO, String] = new RecordingSttpBackend(stubBackend)
    val client = PokemonApiClient(endpoint, cache, stub)

    val response1 = client.pokemonSpecies2(MEW_TWO).unsafeRunSync()
    mockServer.reset()
  }

  "calling pokemon-species with cached request wiremock" should "not call api second time" in {

    stubFor(get(urlEqualTo("/pokemon-species/mewtwo"))
      .willReturn(
        aResponse()
          .withBody(mewTwoPokemonSpeciesResponse.pokemon.asJson.toString)
          .withStatus(200)))

    val stub: RecordingSttpBackend[IO, String] = new RecordingSttpBackend(stubBackend)
    val client = PokemonApiClient(endpoints, cache, stub)

    Thread.sleep(200)

    val response1 = client.pokemonSpecies2(MEW_TWO).unsafeRunSync()
    verify(1, getRequestedFor(urlEqualTo("/pokemon-species/mewtwo")))
    println(response1)
  }
}
