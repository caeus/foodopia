package com.github.caeus.foodopia.view

import java.time.temporal.ChronoUnit

import cats.implicits._
import com.github.caeus.foodopia.model.RestaurantsFilter.{City, Location}
import com.github.caeus.foodopia.logic._
import com.github.caeus.foodopia.model.{LogInReq, Restaurant, RestaurantsFilter, SignUpReq}
import io.circe.generic.AutoDerivation
import sttp.model.CookieValueWithMeta
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio.interop.catz._
import zio.{Runtime, Task}
case class ApiError(message: String)
object FoodopiaDocs extends AutoDerivation {
  import sttp.tapir.docs.openapi._
  import sttp.tapir.json.circe._
  import sttp.tapir.openapi.circe.yaml._
  import sttp.tapir.{endpoint, _}

  val logIn = endpoint.post
    .in("v1" / "guest" / "log-in")
    .in(jsonBody[LogInReq])
    .out(setCookie("session_id"))
    .errorOut(jsonBody[ApiError])

  val signUp = endpoint.post
    .in("v1" / "guest" / "sign-up")
    .in(jsonBody[SignUpReq])
    .out(setCookie("session_id"))
    .errorOut(jsonBody[ApiError])

  val search = endpoint.get
    .in("v1" / "customer" / "restaurants")
    .in(cookie[String]("session_id"))
    .in(query[Option[String]]("city"))
    .in(query[Option[String]]("location"))
    .out(jsonBody[Seq[Restaurant]])
    .errorOut(jsonBody[ApiError])

  val logOut = endpoint
    .in("v1" / "guest" / "log-out")
    .in(cookie[String]("session_id"))
    .out(setCookie("session_id"))
    .errorOut(jsonBody[ApiError])

  val openApi = List(
    logIn,
    signUp,
    search,
    logOut
  ).toOpenAPI("Foodopia", "1.0")
  val yaml = openApi.toYaml
}
class FoodopiaCtrl(foodopiaAPI: FoodopiaAPI, clock: zio.clock.Clock.Service[Any])(
    implicit runtime: Runtime[Any]) {
  import org.http4s.HttpRoutes
  import sttp.tapir.server.http4s._
  private val docs = FoodopiaDocs
  def simpleCookie(value: String) =
    CookieValueWithMeta(value = value,
                        expires = None,
                        maxAge = None,
                        domain = None,
                        path = Some("/"),
                        secure = false,
                        httpOnly = false,
                        otherDirectives = Map.empty)
  private val logIn: HttpRoutes[Task] = docs.logIn.toRoutes { req =>
    (for {
      guestAPI <- foodopiaAPI.guest
      resp     <- guestAPI.logIn(req)
    } yield simpleCookie(resp.token))
      .mapError(e => ApiError(e.getMessage))
      .either
  }

  private val logOut: HttpRoutes[Task] = docs.logOut.toRoutes { token =>
    (for {
      customerAPI <- foodopiaAPI.customer(token)
      _           <- customerAPI.logOut
      yesterday   <- clock.currentDateTime.map(_.toInstant.minus(1, ChronoUnit.DAYS))
    } yield simpleCookie("").copy(expires = Some(yesterday)))
      .mapError(e => ApiError(e.getMessage))
      .either
  }

  private val signUp: HttpRoutes[Task] = docs.signUp.toRoutes { req =>
    (for {
      guestAPI <- foodopiaAPI.guest
      resp     <- guestAPI.signUp(req)
    } yield simpleCookie(resp.token))
      .mapError(e => ApiError(e.getMessage))
      .either
  }
  private val search: HttpRoutes[Task] = docs.search.toRoutes {
    case (token, city, location) =>
      (for {
        filter: RestaurantsFilter <- Task.effect(
          city
            .map(City.apply)
            .orElse(location.map { loc =>
              val List(lat: String, lng: String) = loc.split(",").toList
              Location(BigDecimal(lat), BigDecimal(lng))
            })
            .getOrElse(throw new IllegalArgumentException("Location not provided")))
        customerApi            <- foodopiaAPI.customer(token)
        value: Seq[Restaurant] <- customerApi.restaurantsBy(filter)
      } yield value)
        .mapError(e => ApiError(e.getMessage))
        .either
  }
  val routes = logIn <+> logOut <+> signUp <+> search <+> new SwaggerHttp4s(FoodopiaDocs.yaml)
    .routes[Task]
}
