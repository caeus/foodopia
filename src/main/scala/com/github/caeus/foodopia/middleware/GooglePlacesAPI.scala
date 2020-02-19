package com.github.caeus.foodopia.middleware

import com.github.caeus.foodopia.conf.GPlacesConf
import io.circe
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.{SttpBackend, basicRequest, _}
import sttp.model.Uri.QuerySegment
import zio.Task
import io.circe.generic.auto._
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.circe._
trait GooglePlacesAPI {

  def nearbysearch(lat:BigDecimal,lng:BigDecimal): Task[Seq[GRestaurant]]
}
case class GResults(results:Seq[GRestaurant])
case class GLocation(lat:BigDecimal,lng:BigDecimal)
case class GGeometry(location:GLocation)
case class GRestaurant(name:String,
                       formatted_address:Option[String],geometry: GGeometry)
object GooglePlacesAPI {
  def impl(conf:GPlacesConf)(implicit sttpBackend: SttpBackend[Task, Nothing, WebSocketHandler]): GooglePlacesAPI =
    new DefaultGooglePlacesAPI(conf)
}
//https://wft-geo-db.p.mashape.com/v1/geo/cities?namePrefix=bogot&types=CITY
final class DefaultGooglePlacesAPI(conf:GPlacesConf)(
    implicit sttpBackend: SttpBackend[Task, Nothing, WebSocketHandler])
    extends GooglePlacesAPI {
  override def nearbysearch(lat:BigDecimal,lng:BigDecimal): Task[Seq[GRestaurant]] = {
    basicRequest
      .get(
        uri"https://maps.googleapis.com/maps/api/place/nearbysearch/json"
          .querySegment(QuerySegment.KeyValue("key", "AIzaSyD1iAN6cHC8tvOlBmRrNixh_L3RoP6UWpQ"))
          .querySegment(QuerySegment.KeyValue("location", s"$lat,$lng"))
          .querySegment(QuerySegment.KeyValue("type", "restaurant"))
          .querySegment(QuerySegment.KeyValue("rankby", "distance"))
      )
      .response(asJson[GResults])
      .send()
      .map(_.body)
      .flatMap {
        case Left(serError: ResponseError[circe.Error]) =>
          serError match {
            case DeserializationError(or,_)=>

              println(or)
            case _ =>
          }
          serError.printStackTrace()
          Task.fail(serError)
        case Right(data) =>
          Task.succeed(data.results)
      }
  }
}
