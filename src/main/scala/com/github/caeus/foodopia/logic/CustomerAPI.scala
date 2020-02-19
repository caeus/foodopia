package com.github.caeus.foodopia.logic

import com.github.caeus.foodopia.model.RestaurantsFilter.{City, Location}
import com.github.caeus.foodopia.middleware.GeoCitiesDB
import com.github.caeus.foodopia.model.{Restaurant, RestaurantsFilter}
import zio.Task

trait CustomerAPI {
  def restaurantsBy(filter: RestaurantsFilter): Task[Seq[Restaurant]]
  def logOut: Task[Unit]
}
object CustomerAPI {
  def impl(email: String,
           nearbyRestaurantsSrv: NearbyRestaurantsSrv,
           geoCitiesDB: GeoCitiesDB): CustomerAPI =
    new DefaultCustomerAPI(email, nearbyRestaurantsSrv, geoCitiesDB: GeoCitiesDB)
}
final class DefaultCustomerAPI(email: String,
                               nearbyRestaurantsSrv: NearbyRestaurantsSrv,
                               geoCitiesDB: GeoCitiesDB)
  extends CustomerAPI {
  override def restaurantsBy(filter: RestaurantsFilter): Task[Seq[Restaurant]] = {
    filter match {
      case City(name) =>
        for {
          city <- geoCitiesDB.firstByPrefix(name).flatMap {
            case Some(city) => Task.succeed(city)
            case None       => Task.fail(new IllegalArgumentException(s"There's no city with name $name"))
          }
          resaurants <- nearbyRestaurantsSrv.byLatLng(city.latitude, city.longitude)
        } yield resaurants
      case Location(lat, lng) =>
        nearbyRestaurantsSrv.byLatLng(lat, lng)
    }
  }

  override def logOut: Task[Unit] = Task.succeed(())
}