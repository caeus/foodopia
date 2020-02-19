package com.github.caeus.foodopia

import com.github.caeus.foodopia.conf.FoodopiaConf
import com.github.caeus.foodopia.logic.{FoodopiaAPI, RestaurantsEngine}
import com.github.caeus.foodopia.middleware.{AuthEngine, GeoCitiesDB, GooglePlacesAPI}
import com.github.caeus.foodopia.storage.{BootstrapDB, CustomerRepo, SearchesRegistry}
import com.github.caeus.foodopia.view.FoodopiaCtrl
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, Logger}
import sttp.client.SttpBackend
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.clock.Clock
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.{FiberFailure, RIO, Runtime, Task, URIO, ZEnv, ZIO, ZManaged}
class FoodopiaMain(implicit runtime: zio.Runtime[Any]) {
  def wire: ZManaged[ZEnv, Throwable, Int] = AsyncHttpClientZioBackend().toManaged_.flatMap {
    implicit backend: SttpBackend[Task, Nothing, WebSocketHandler] =>
      for {
        conf                      <- FoodopiaConf.load
        clock: Clock.Service[Any] <- RIO.access[ZEnv](_.clock).toManaged_
        db                        <- BootstrapDB.prod(conf.db)
        googlePlacesAPI            = GooglePlacesAPI.impl(conf.middleware.gPlaces)
        geoCitiesDB                = GeoCitiesDB.impl(conf.middleware.geoCities)
        restaurantsEngine          = RestaurantsEngine.impl(googlePlacesAPI, geoCitiesDB)
        customerRepo: CustomerRepo = CustomerRepo.impl(db)
        authEngine: AuthEngine     = AuthEngine.impl(clock, conf.auth)
        searchesEngine             = SearchesRegistry.impl(db, clock)
        foodopiaAPI = FoodopiaAPI.impl(customerRepo,
                                       authEngine,
                                       restaurantsEngine,
                                       searchesEngine,
                                       geoCitiesDB)
        foodopiaCtrl = new FoodopiaCtrl(foodopiaAPI, clock)
        httpApp = Logger.httpApp(logHeaders = false, logBody = false) {
          CORS(foodopiaCtrl.routes.orNotFound,
               CORS.DefaultCORSConfig.copy(allowedOrigins = _ => true))
        }
        _ <- BlazeServerBuilder[Task]
          .bindHttp(conf.http.port, conf.http.host)
          .withHttpApp(httpApp)
          .serve
          .compile
          .drain
          .toManaged_
      } yield 0
  }
  val run: URIO[ZEnv, Int] = wire
    .use(_ => URIO.unit)
    .foldCauseM({ cause =>
      ZIO.effectTotal(FiberFailure(cause).printStackTrace()).as(1)
    }, _ => ZIO.succeed(0))
}

object Main extends zio.App {
  implicit val rtm: Runtime[ZEnv] = this

  def run(args: List[String]): URIO[ZEnv, Int] =
    new FoodopiaMain().run
}
