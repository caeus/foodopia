package com.github.caeus.foodopia.logic

import com.github.caeus.foodopia.middleware.{GeoCitiesDB, GooglePlacesAPI}
import com.github.caeus.foodopia.model.RestaurantsFilter.{City, Location}
import com.github.caeus.foodopia.model.{RLocation, Restaurant, RestaurantsFilter}
import zio.Task

trait RestaurantsEngine {
  def byFilter(filter: RestaurantsFilter): Task[Seq[Restaurant]]
}
object RestaurantsEngine {
  def impl(googlePlacesAPI: GooglePlacesAPI, geoCitiesDB: GeoCitiesDB): RestaurantsEngine =
    new DefaultRestaurantsEngine(googlePlacesAPI, geoCitiesDB: GeoCitiesDB)
}
final class DefaultRestaurantsEngine(googlePlacesAPI: GooglePlacesAPI, geoCitiesDB: GeoCitiesDB)
    extends RestaurantsEngine {
  override def byFilter(filter: RestaurantsFilter): Task[Seq[Restaurant]] = {
    filter match {
      case City(name) =>
        for {
          city <- geoCitiesDB.firstByPrefix(name).flatMap {
            case Some(city) => Task.succeed(city)
            case None       => Task.fail(new IllegalArgumentException(s"There's no city with name $name"))
          }
          resaurants <- byLatLng(city.latitude, city.longitude)
        } yield resaurants
      case Location(lat, lng) =>
        byLatLng(lat, lng)
    }

  }
  def byLatLng(lat: BigDecimal, lng: BigDecimal): Task[Seq[Restaurant]] = {
    googlePlacesAPI.nearbysearch(lat, lng).map { grestaurants =>
      grestaurants.iterator
        .map(
          r =>
            Restaurant(r.name,
                       RLocation(
                         formatted = r.formatted_address,
                         lat = r.geometry.location.lat,
                         lng = r.geometry.location.lng
                       )))
        .toSeq
    }
  }

}
