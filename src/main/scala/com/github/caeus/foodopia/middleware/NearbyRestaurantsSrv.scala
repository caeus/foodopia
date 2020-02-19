package com.github.caeus.foodopia.middleware

import com.github.caeus.foodopia.logic.Restaurant
import zio.Task

trait NearbyRestaurantsSrv {
  def byLatLng(lat: BigDecimal, lng: BigDecimal): Task[Seq[Restaurant]]
}
object NearbyRestaurantsSrv {
  def impl(googlePlacesAPI: GooglePlacesAPI): NearbyRestaurantsSrv =
    new DefaultNearbyRestaurantsSrv(googlePlacesAPI)
}
final class DefaultNearbyRestaurantsSrv(googlePlacesAPI: GooglePlacesAPI)
    extends NearbyRestaurantsSrv {
  override def byLatLng(lat: BigDecimal, lng: BigDecimal): Task[Seq[Restaurant]] = Task.succeed(Nil)
}
