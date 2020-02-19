package com.github.caeus.foodopia.view

import cats.implicits._
import com.github.caeus.foodopia.logic.RestaurantsFilter.City
import com.github.caeus.foodopia.logic.{FoodopiaAPI, LogInReq, Restaurant, SignUpReq}
import io.circe.generic.AutoDerivation
import sttp.model.CookieValueWithMeta
import zio.interop.catz._
import zio.{Runtime, Task}
case class ApiError(message: String)
object FoodopiaDocs extends AutoDerivation {
  //import sttp.tapir.docs.openapi._
  import sttp.tapir.json.circe._
  //import sttp.tapir.openapi.circe.yaml._
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
    .out(jsonBody[Seq[Restaurant]])
    .errorOut(jsonBody[ApiError])
}
class FoodopiaCtrl(foodopiaAPI: FoodopiaAPI)(implicit runtime: Runtime[Any]) {
  import org.http4s.HttpRoutes
  import sttp.tapir.server.http4s._
  private val docs = FoodopiaDocs
  private val logIn: HttpRoutes[Task] = docs.logIn.toRoutes { req =>
    (for {
      guestAPI <- foodopiaAPI.guest
      resp     <- guestAPI.logIn(req)
    } yield
      CookieValueWithMeta(value = resp.token,
                          expires = None,
                          maxAge = None,
                          domain = None,
                          path = Some("/"),
                          secure = false,
                          httpOnly = false,
                          otherDirectives = Map.empty))
      .mapError(e => ApiError(e.getMessage))
      .either
  }

  private val signUp: HttpRoutes[Task] = docs.signUp.toRoutes { req =>
    (for {
      guestAPI <- foodopiaAPI.guest
      resp     <- guestAPI.signUp(req)
    } yield
      CookieValueWithMeta(value = resp.token,
                          expires = None,
                          maxAge = None,
                          domain = None,
                          path = Some("/"),
                          secure = false,
                          httpOnly = false,
                          otherDirectives = Map.empty))
      .mapError(e => ApiError(e.getMessage))
      .either
  }
  private val search: HttpRoutes[Task] = docs.search.toRoutes { token =>
    (for {
      customerApi            <- foodopiaAPI.customer(token)
      value: Seq[Restaurant] <- customerApi.restaurantsBy(City("alksjd"))
    } yield value)
      .mapError(e => ApiError(e.getMessage))
      .either
  }
  val routes = logIn <+> signUp <+> search
}
