package com.github.caeus.foodopia.conf

import java.net.URI

import pureconfig.error.ConfigReaderException
import zio.{TaskManaged, ZIO}

case class AuthConf(privateKey: String)
case class HttpConf(port: Int, host: String)
case class GeoCitiesConf(apiKey: String)
case class GPlacesConf(apiKey: String)
case class MiddlewareConf(geoCities: GeoCitiesConf, gPlaces: GPlacesConf)
case class DBConnectionConfig(url: String) {
  private val uri      = new URI(url)
  private val info     = uri.getUserInfo.split(":").toList
  val jdbcUrl: String  = s"jdbc:postgresql://${uri.getHost}:${uri.getPort}${uri.getPath}"
  val password: String = info(1)
  val user: String     = info.head
}
case class FoodopiaConf(auth: AuthConf,
                        http: HttpConf,
                        middleware: MiddlewareConf,
                        db: DBConnectionConfig)
object FoodopiaConf {
  def load: TaskManaged[FoodopiaConf] = {
    import pureconfig._
    import pureconfig.generic.auto._
    ZIO
      .fromEither(ConfigSource.default.load[FoodopiaConf])
      .mapError { x =>
        ConfigReaderException(x)
      }
      .absorb
      .toManaged(zio.URIO.succeed)
  }
}
