package com.github.caeus.foodopia.middleware

import zio.Task

trait GooglePlacesAPI {

  def findplacefromtext(params: Map[String, String]): Task[Any]
}
object GooglePlacesAPI {
  def impl: GooglePlacesAPI = new DefaultGooglePlacesAPI
}
final class DefaultGooglePlacesAPI extends GooglePlacesAPI {
  override def findplacefromtext(params: Map[String, String]): Task[Any] = ???
}
