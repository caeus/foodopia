package com.github.caeus.foodopia.conf

import pureconfig.error.ConfigReaderException
import zio.{Task, ZIO}

case class AuthConf(privateKey: String)
case class HttpConf(port: Int, host: String)
case class GeoCitiesConf(apiKey: String)
case class GPlacesConf(apiKey: String)
case class MiddlewareConf(geoCities: GeoCitiesConf, gPlaces: GPlacesConf)

case class FoodopiaConf(auth: AuthConf, http: HttpConf, middleware: MiddlewareConf)
object FoodopiaConf {
  def load: Task[FoodopiaConf] = {
    import pureconfig._
    import pureconfig.generic.auto._
    ZIO
      .fromEither(ConfigSource.default.load[FoodopiaConf])
      .mapError { x =>
        ConfigReaderException(x)
      }
      .absorb
  }
}
