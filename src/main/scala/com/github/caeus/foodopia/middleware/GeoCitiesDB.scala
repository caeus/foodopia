package com.github.caeus.foodopia.middleware

import com.github.caeus.foodopia.conf.GeoCitiesConf
import io.circe
import io.circe.generic.auto._
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.circe._
import sttp.client.{SttpBackend, basicRequest, _}
import sttp.model.Uri.QuerySegment
import zio.Task

case class Response(data: Seq[City])
case class City(
    name: String,
    country: String,
    latitude: BigDecimal,
    longitude: BigDecimal
)
trait GeoCitiesDB {

  def firstByPrefix(prefix: String): Task[Option[City]]

}
object GeoCitiesDB {
  def impl(geoCitiesConf: GeoCitiesConf)(
      implicit sttpBackend: SttpBackend[Task, Nothing, WebSocketHandler]): GeoCitiesDB =
    new DefaultGeoCitiesDB(geoCitiesConf: GeoCitiesConf)
}
final class DefaultGeoCitiesDB(geoCitiesConf: GeoCitiesConf)(
    implicit sttpBackend: SttpBackend[Task, Nothing, WebSocketHandler])
    extends GeoCitiesDB {

  def firstByPrefix(prefix: String): Task[Option[City]] = {
    basicRequest
      .get(
        uri"https://wft-geo-db.p.mashape.com/v1/geo/cities"
          .querySegment(QuerySegment.KeyValue("types", "CITY"))
          .querySegment(QuerySegment.KeyValue("namePrefix", prefix)))
      .header("X-Mashape-Key", "a0a17a989bmsh623ec26166d412fp17e41ejsnb759ffa15f83")
      .response(asJson[Response])
      .send()
      .map(_.body)
      .flatMap {
        case Left(serError: ResponseError[circe.Error]) =>
          Task.fail(serError)
        case Right(data: Response) =>
          Task.succeed(data.data.headOption)
      }
  }

}
