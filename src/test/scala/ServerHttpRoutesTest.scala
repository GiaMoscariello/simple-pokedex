import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.gia.moscariello.simple.pokedex.http.ServerRoutes
import com.gia.moscariello.simple.pokedex.models._
import com.gia.moscariello.simple.pokedex.persistence.{HttpCache, InMemoryCache}
import com.gia.moscariello.simple.pokedex.services.{Engine, PokemonApiClient, TranslationApiClient}
import com.github.tomakehurst.wiremock.client.WireMock
import org.http4s.Method.GET
import org.http4s.Request
import org.http4s.implicits.{http4sKleisliResponseSyntaxOptionT, http4sLiteralsSyntax}
import org.http4s.server.Router
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class ServerHttpRoutesTest extends AnyFlatSpec with BeforeAndAfterEach {
  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  val cache: HttpCache[String, String] = new InMemoryCache()
  val host = sys.env.getOrElse("WIREMOCK_HOST", "localhost")

  WireMock.configureFor(host, 8080)

  val pokemonEndpoints = PokemonApiEndpoints(baseUrl = s"http://${host}:8080", pokemonSpecies = "pokemon-species")
  val pokemonClient: PokemonApiClient = PokemonApiClient(pokemonEndpoints, cache)

  val translationEndpoints = TranslationEndpoints(baseUrl = s"http://${host}:8080/translate", yoda="yoda.json", shakespeare = "shakespeare.json")
  val translationClient: TranslationApiClient = TranslationApiClient(translationEndpoints, cache)

  val externalEndpoints = ServerHttpConfig("pokemon",  "translated", 8080, host)
  val engine = new Engine(pokemonClient, translationClient)
  val server = new ServerRoutes(
    externalEndpoints,
    engine
  )

  val routes = server.routes
  val api = Router("/" -> routes).orNotFound
  val bodyCorrectResponse = "{\"name\":\"mewtwo\",\"description\":\"It was created by a scientist after years of horrific gene splicing and DNA engineering experiments.\",\"habitat\":\"rare\",\"is_legendary\":true}"
  val translatedResponse = "{\"name\":\"mewtwo\",\"description\":\"Created by a scientist after years of horrific gene splicing and dna engineering experiments,  it was.\",\"habitat\":\"rare\",\"is_legendary\":true}"

  override def afterEach(): Unit = {
    WireMock.resetAllRequests()
  }

  "calling /pokemon routes with correct input" should "return correctly with status 200" in {
    val result = api.run(Request(method = GET, uri = uri"/pokemon?name=mewtwo")).unsafeRunSync()

    println(result.bodyText.compile.string.unsafeRunSync())
    assert(result.status.code == 200)
    assert(result.bodyText.compile.string.unsafeRunSync() == bodyCorrectResponse)
  }

  "calling /pokemon/translated routes with correct input" should "return correctly with status 200" in {
    val result = api.run(Request(method = GET, uri=uri"/translated?name=mewtwo")).unsafeRunSync()

    assert(result.status.code == 200)
    assert(result.bodyText.compile.string.unsafeRunSync() == translatedResponse)
  }

  "calling /pokemon routes with wrong input" should "return not found 404" in {
    val result = api.run(Request(method = GET, uri=uri"/pokemon?name=not-a-pokemon")).unsafeRunSync()

    assert(result.status.code == 404)
  }

}
