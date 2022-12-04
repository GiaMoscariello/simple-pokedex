import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.gia.moscariello.simple.pokedex.models._
import com.gia.moscariello.simple.pokedex.persistence.{HttpCache, InMemoryCache}
import com.gia.moscariello.simple.pokedex.services.TranslationApiClient
import com.github.tomakehurst.wiremock.client.WireMock.{equalToJson, postRequestedFor, urlEqualTo, verify}
import com.github.tomakehurst.wiremock.client.{CountMatchingStrategy, WireMock}
import io.circe.syntax.EncoderOps
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class TranslationApiClientTest extends AnyFlatSpec with BeforeAndAfterEach {
  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  val host = sys.env.getOrElse("WIREMOCK_HOST", "localhost")

  val endpoints = TranslationEndpoints(baseUrl = s"http://${host}:8080/translate", yoda="yoda.json", shakespeare = "shakespeare.json")

  val cache: HttpCache[String, String] = new InMemoryCache()

  WireMock.configureFor(host, 8080)

  val stub: TranslationApiClient = TranslationApiClient(endpoints, cache)

  val SHAKESPEARE_TRANSLATED =
    "At which hour several of these pokémon gather,  their electricity couldst buildeth and cause lightning storms"

  val YODA_TRANSLATED =
    "Created by a scientist after years of horrific gene splicing and dna engineering experiments,  it was."

  val SHAKESPEARE_REQUEST: TranslationRequest =
    TranslationRequest("When several of these POKéMON gather, their electricity could build and cause lightning storms")

  val YODA_REQUEST: TranslationRequest =
    TranslationRequest("It was created by a scientist after years of horrific gene splicing and DNA engineering experiments.")

  val WRONG_SHAKESPEARE_REQUEST: TranslationRequest =
    TranslationRequest("This POKéMON has electricity-storing pouches on its cheeks. These appear to become electrically charged during the\\fnight while PIKACHU sleeps. It occasionally discharges electricity when it is dozy after waking up.")

  val WRONG_YODA_REQUEST: TranslationRequest =
    TranslationRequest("It was created by a scientist after years of horrific\\fgene splicing and DNA engineering experiments.")

  val SHAKESPEARE_TRANSLATED_RESPONSE: TranslationResponseSuccess = TranslationResponseSuccess(contents = Contents(
    translated = SHAKESPEARE_TRANSLATED, text = SHAKESPEARE_REQUEST.text, translation = "shakespeare"
  ))

  val YODA_TRANSLATED_RESPONSE: TranslationResponseSuccess = TranslationResponseSuccess(contents = Contents(
    translated = YODA_TRANSLATED, text = YODA_REQUEST.text, translation = "yoda"
  ))

  private def stubbedCache: HttpCache[String, String] = {
    val stubbedCache: HttpCache[String, String] = new InMemoryCache()

    stubbedCache.put(SHAKESPEARE_REQUEST.asJson.toString, SHAKESPEARE_TRANSLATED_RESPONSE.asJson.toString)
    stubbedCache.put(YODA_REQUEST.asJson.toString, YODA_TRANSLATED_RESPONSE.asJson.toString)
    stubbedCache
  }

  override def beforeEach(): Unit = {
    WireMock.resetAllRequests()
  }

  "calling translation shakespeare api with correct input" should "return 200" in {
    val response = stub.shakespeare(SHAKESPEARE_REQUEST).unsafeRunSync()

    verify(1, postRequestedFor(urlEqualTo("/translate/shakespeare.json")))
    response match {
      case actual: TranslationResponseSuccess => assert(actual.contents.translated == SHAKESPEARE_TRANSLATED)
      case err: TranslationResponseError => fail(err.error.toString)
    }
  }

  "calling translation yoda api with correct input" should "return 200" in {
    val response = stub.yoda(YODA_REQUEST).unsafeRunSync()

    verify(1, postRequestedFor(urlEqualTo("/translate/yoda.json")))
    response match {
      case actual: TranslationResponseSuccess => assert(actual.contents.translated == YODA_TRANSLATED)
      case err: TranslationResponseError => fail(err.error.toString)
    }
  }

  "calling translation shakespeare api with not valid chars" should "return 400 Bad Request" in {
      val response = stub.shakespeare(WRONG_SHAKESPEARE_REQUEST).unsafeRunSync()

    verify(1, postRequestedFor(urlEqualTo("/translate/shakespeare.json"))
      .withRequestBody(equalToJson(WRONG_SHAKESPEARE_REQUEST.asJson.toString)).but()
    )

    response match {
      case err: TranslationResponseError => assert(err.error.code == 400)
      case _:TranslationResponseSuccess => fail("Api response correctly")
    }
  }

  "calling translation yoda api with not valid chars" should "return 400 Bad Request" in {
    val response = stub.yoda(WRONG_YODA_REQUEST).unsafeRunSync()

    verify(1, postRequestedFor(urlEqualTo("/translate/yoda.json"))
      .withRequestBody(equalToJson(WRONG_YODA_REQUEST.asJson.toString))
    )

    response match {
      case err: TranslationResponseError => assert(err.error.code == 400)
      case _:TranslationResponseSuccess => fail("Api response correctly")
    }
  }

  "calling translation shakespeare api with cached result" should "return 200 and should not call api again" in {
    val stubWithCache = TranslationApiClient(endpoints, stubbedCache)
    val response = stubWithCache.shakespeare(SHAKESPEARE_REQUEST).unsafeRunSync()

    verify(0, postRequestedFor(urlEqualTo("/translate/shakespeare.json")))
    response match {
      case actual: TranslationResponseSuccess => assert(actual.contents.translated == SHAKESPEARE_TRANSLATED)
      case err: TranslationResponseError => fail(err.error.toString)
    }
  }

  "calling translation yoda api with cached result" should "return 200 and should not call api again" in {
    val stubWithCache = TranslationApiClient(endpoints, stubbedCache)
    val response = stubWithCache.shakespeare(YODA_REQUEST).unsafeRunSync()

    verify(0, postRequestedFor(urlEqualTo("/translate/yoda.json")))
    response match {
      case actual: TranslationResponseSuccess => assert(actual.contents.translated == YODA_TRANSLATED)
      case err: TranslationResponseError => fail(err.error.toString)
    }
  }
}
