package com.github.caeus.foodopia.conf

import pureconfig.error.ConfigReaderException
import zio.{Task, ZIO}

case class FoodopiaAuth(privateKey: String)
case class FoodopiaConf(auth: FoodopiaAuth)

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
