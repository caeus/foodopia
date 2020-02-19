package com.github.caeus.foodopia.logic

import com.github.caeus.foodopia.middleware.GooglePlacesAPI
import com.github.caeus.foodopia.model.{RLocation, Restaurant}
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
  override def byLatLng(lat: BigDecimal, lng: BigDecimal): Task[Seq[Restaurant]] =
    googlePlacesAPI.nearbysearch(lat, lng).map { grestaurants =>
      grestaurants.iterator.map(r => Restaurant(r.name,RLocation(
        formatted = r.formatted_address,
        lat=r.geometry.location.lat,
        lng=r.geometry.location.lng
      ))).toSeq
    }
}
