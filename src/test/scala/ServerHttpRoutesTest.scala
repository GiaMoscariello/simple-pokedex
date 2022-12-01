import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.github.blemale.scaffeine.{Cache, Scaffeine}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import http.ServerRoutes
import models.{ExposedRoutes, PokemonApiEndpoints, TranslationEndpoints}
import org.http4s.LiteralSyntaxMacros.uri
import org.http4s.Method.GET
import org.http4s.{Request, Uri}
import org.http4s.implicits.{http4sKleisliResponseSyntaxOptionT, http4sLiteralsSyntax}
import org.http4s.server.Router
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import persistence.{HttpCache, InMemoryCache}
import services.{Engine, PokemonApiClient, TranslationApiClient}

import java.nio.charset.StandardCharsets.UTF_8
import java.net.URLEncoder

class ServerHttpRoutesTest extends AnyFlatSpec with BeforeAndAfterEach {
  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  val wireMockServer = new WireMockServer(WireMockConfiguration.options().
    port(8080).
    usingFilesUnderDirectory("./src/test/resources"))

  val cache: HttpCache[String, String] = new InMemoryCache()

  val pokemonEndpoints = PokemonApiEndpoints(baseUrl = s"http://localhost:8080", pokemonSpecies = "pokemon-species")
  val pokemonClient: PokemonApiClient = services.PokemonApiClient(pokemonEndpoints, cache)

  val translationEndpoints = TranslationEndpoints(baseUrl = s"http://localhost:8080/translate", yoda="yoda.json", shakespeare = "shakespeare.json")
  val translationClient: TranslationApiClient = services.TranslationApiClient(translationEndpoints, cache)

  val externalEndpoints = ExposedRoutes("pokemon",  "translated")
  val engine = new Engine(pokemonClient, translationClient)
  val server = new ServerRoutes(
    externalEndpoints,
    engine
  )

  val routes = server.routes
  val api = Router("/" -> routes).orNotFound
  val bodyCorrectResponse = "{\"name\":\"mewtwo\",\"description\":\"It was created by a scientist after years of horrific gene splicing and DNA engineering experiments.\",\"habitat\":\"rare\",\"is_legendary\":true}"
  val translatedResponse = "{\"name\":\"mewtwo\",\"description\":\"Created by a scientist after years of horrific gene splicing and dna engineering experiments,  it was.\",\"habitat\":\"rare\",\"is_legendary\":true}"

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
    val result = api.run(Request(method = GET, uri=uri"/pokemon?name=f")).unsafeRunSync()

    assert(result.status.code == 404)
  }

}
