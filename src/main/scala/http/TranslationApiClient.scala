package http

import cats.effect.IO
import com.github.blemale.scaffeine.Cache
import io.circe.syntax.EncoderOps
import models.{TranslationEndpoints, TranslationRequest, TranslationResponse, TranslationResponseError, TranslationResponseSuccess}
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.{Identity, RequestT, UriContext, basicRequest}
import io.circe.parser.decode
import org.typelevel.log4cats.Logger

case class TranslationApiClient(endpoint: TranslationEndpoints, private val cache: Cache[String, String])(implicit logger: Logger[IO]) {

  def shakespeare(bodyRequest: TranslationRequest): IO[TranslationResponse] = {
    val uri = s"${endpoint.shakespeareEndpoint}"
    getResult(bodyRequest, uri)
  }

  def yoda(bodyRequest: TranslationRequest): IO[TranslationResponse] = {
    val uri = s"${endpoint.yodaEndpoint}"
    getResult(bodyRequest, uri)
  }

  private def buildRequest(bodyRequest: TranslationRequest, uri: String) = {
    basicRequest
      .post(uri"${uri}")
      .body(bodyRequest.asJson.toString)
      .contentType("text/json", "utf-8")
  }

  private def getResult(bodyRequest: TranslationRequest, uri: String): IO[TranslationResponse] = {
    cache.getIfPresent(bodyRequest.asJson.toString) match {
      case Some(response) => IO.fromEither(handleCacheResult(response))
      case None => sendRequest(bodyRequest, uri)
    }
  }

  private def handleCacheResult(response: String): Either[io.circe.Error, TranslationResponse] = {
    decode[TranslationResponseSuccess](response) match {
      case Left(_) => decode[TranslationResponseError](response)
      case Right(success) => Right(success)
    }
  }

  private def sendRequest(bodyRequest: TranslationRequest,  uri: String): IO[TranslationResponse] = {
    AsyncHttpClientCatsBackend
      .resource[IO]()
      .use(backend =>
        buildRequest(bodyRequest, uri)
          .send(backend)
          .flatMap(_.body.fold[IO[TranslationResponse]](
            error => {
              cache.put(bodyRequest.asJson.toString, error)
              logger.info(error) *>
              IO.fromEither(decode[TranslationResponseError](error))
            },
            success => {
              cache.put(bodyRequest.asJson.toString, success)
              IO.fromEither(decode[TranslationResponseSuccess](success))
            }
          ))
      )
  }
}
