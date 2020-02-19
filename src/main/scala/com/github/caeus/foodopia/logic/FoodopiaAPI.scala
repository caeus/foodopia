package com.github.caeus.foodopia.logic

import com.github.caeus.foodopia.logic.RestaurantsFilter.{City, Location}
import com.github.caeus.foodopia.middleware.{AuthEngine, GeoCitiesDB, NearbyRestaurantsSrv}
import com.github.caeus.foodopia.storage.{CustomerRepo, CustomerRow}
import zio.Task

case class LogInReq(email: String, password: String)
case class LogInResp(token: String)
case class SignUpReq(name: String, email: String, password: String)
case class SignUpResp(token: String)
trait GuestAPI {
  def logIn(req: LogInReq): Task[LogInResp]
  def signUp(req: SignUpReq): Task[SignUpResp]
}
object GuestAPI {
  def impl(customerRepo: CustomerRepo, authEngine: AuthEngine): GuestAPI =
    new DefaultGuestAPI(customerRepo: CustomerRepo, authEngine: AuthEngine)
}
final class DefaultGuestAPI(customerRepo: CustomerRepo, authEngine: AuthEngine) extends GuestAPI {

  override def logIn(req: LogInReq): Task[LogInResp] = {
    for {
      _ <- customerRepo.byEmail(req.email).flatMap {
        case None =>
          Task.fail(new IllegalArgumentException(s"Email ${req.email} is not registered"))
        case Some(row) => Task.succeed(row)
      }
      token <- authEngine.toToken(req.email)
    } yield LogInResp(token)
  }

  override def signUp(req: SignUpReq): Task[SignUpResp] = {
    for {
      _ <- customerRepo
        .byEmail(req.email)
        .flatMap {
          case Some(_) =>
            Task.fail(new IllegalArgumentException(s"Email ${req.email} is already registered"))
          case None => Task.succeed(())
        }
      hashpw <- authEngine.hashPw(req.password)
      _      <- customerRepo.create(CustomerRow(req.name, req.email, hashpw))
      token  <- authEngine.toToken(req.email)
    } yield SignUpResp(token)
  }
}
sealed trait RestaurantsFilter
object RestaurantsFilter {
  case class City(name: String)                         extends RestaurantsFilter
  case class Location(lat: BigDecimal, lng: BigDecimal) extends RestaurantsFilter
}
case class RLocation(formatted:Option[String],lat:BigDecimal,lng:BigDecimal)
case class Restaurant(name:String, location:RLocation)

trait CustomerAPI {
  def restaurantsBy(filter: RestaurantsFilter): Task[Seq[Restaurant]]
  def logOut: Task[Unit]
}
object CustomerAPI {
  def impl(email: String,
           nearbyRestaurantsSrv: NearbyRestaurantsSrv,
           geoCitiesDB: GeoCitiesDB): CustomerAPI =
    new DefaultCustomerAPI(email, nearbyRestaurantsSrv, geoCitiesDB: GeoCitiesDB)
}
final class DefaultCustomerAPI(email: String,
                               nearbyRestaurantsSrv: NearbyRestaurantsSrv,
                               geoCitiesDB: GeoCitiesDB)
    extends CustomerAPI {
  override def restaurantsBy(filter: RestaurantsFilter): Task[Seq[Restaurant]] = {
    filter match {
      case City(name) =>
        for {
          city <- geoCitiesDB.firstByPrefix(name).flatMap {
            case Some(city) => Task.succeed(city)
            case None       => Task.fail(new IllegalArgumentException(s"There's no city with name $name"))
          }
          resaurants <- nearbyRestaurantsSrv.byLatLng(city.latitude, city.longitude)
        } yield resaurants
      case Location(lat, lng) =>
        nearbyRestaurantsSrv.byLatLng(lat, lng)
    }
  }

  override def logOut: Task[Unit] = Task.succeed(())
}
trait FoodopiaAPI {
  def guest: Task[GuestAPI]
  def customer(token: String): Task[CustomerAPI]
}

object FoodopiaAPI {
  def impl(customerRepo: CustomerRepo,
           authEngine: AuthEngine,
           nearbyRestaurantsSrv: NearbyRestaurantsSrv,
           geoCitiesDB: GeoCitiesDB): FoodopiaAPI =
    new DefaultFoodopiaAPI(customerRepo: CustomerRepo,
                           authEngine: AuthEngine,
                           nearbyRestaurantsSrv: NearbyRestaurantsSrv,
                           geoCitiesDB: GeoCitiesDB)
}
final class DefaultFoodopiaAPI(customerRepo: CustomerRepo,
                               authEngine: AuthEngine,
                               nearbyRestaurantsSrv: NearbyRestaurantsSrv,
                               geoCitiesDB: GeoCitiesDB)
    extends FoodopiaAPI {

  private val guestAPI = GuestAPI.impl(customerRepo, authEngine)

  override def guest: Task[GuestAPI] = Task.succeed(guestAPI)

  override def customer(token: String): Task[CustomerAPI] = {
    for {
      email <- authEngine.toEmail(token)
      _ <- customerRepo.byEmail(email).flatMap {
        case None    => Task.fail(new IllegalArgumentException(s"Email $email is not registered"))
        case Some(_) => Task.succeed(())
      }
    } yield CustomerAPI.impl(email, nearbyRestaurantsSrv, geoCitiesDB)
  }
}
