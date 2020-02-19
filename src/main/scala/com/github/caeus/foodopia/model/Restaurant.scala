package com.github.caeus.foodopia.model

case class LogInReq(email: String, password: String)
case class LogInResp(token: String)
case class SignUpReq(name: String, email: String, password: String)
case class SignUpResp(token: String)

sealed trait RestaurantsFilter
object RestaurantsFilter {
  case class City(name: String)                         extends RestaurantsFilter
  case class Location(lat: BigDecimal, lng: BigDecimal) extends RestaurantsFilter
}
case class RLocation(formatted:Option[String],lat:BigDecimal,lng:BigDecimal)
case class Restaurant(name:String, location:RLocation)
