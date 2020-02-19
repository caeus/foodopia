package com.github.caeus.foodopia.logic

import com.github.caeus.foodopia.middleware.{AuthEngine, NearbyRestaurantsSrv}
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
case class Restaurant()

trait CustomerAPI {
  def restaurantsBy(filter: RestaurantsFilter): Task[Seq[Restaurant]]
}
object CustomerAPI {
  def impl(nearbyRestaurantsSrv: NearbyRestaurantsSrv): CustomerAPI =
    new DefaultCustomerAPI(nearbyRestaurantsSrv)
}
final class DefaultCustomerAPI(nearbyRestaurantsSrv: NearbyRestaurantsSrv) extends CustomerAPI {
  override def restaurantsBy(filter: RestaurantsFilter): Task[Seq[Restaurant]] =
    nearbyRestaurantsSrv.byLatLng(null, null)
}
trait FoodopiaAPI {
  def guest: Task[GuestAPI]
  def customer(token: String): Task[CustomerAPI]
}

object FoodopiaAPI {
  def impl(customerRepo: CustomerRepo,
           authEngine: AuthEngine,
           nearbyRestaurantsSrv: NearbyRestaurantsSrv): FoodopiaAPI =
    new DefaultFoodopiaAPI(customerRepo: CustomerRepo,
                           authEngine: AuthEngine,
                           nearbyRestaurantsSrv: NearbyRestaurantsSrv)
}
final class DefaultFoodopiaAPI(customerRepo: CustomerRepo,
                               authEngine: AuthEngine,
                               nearbyRestaurantsSrv: NearbyRestaurantsSrv)
    extends FoodopiaAPI {

  private val customerAPI = CustomerAPI.impl(nearbyRestaurantsSrv)
  private val guestAPI    = GuestAPI.impl(customerRepo, authEngine)

  override def guest: Task[GuestAPI] = Task.succeed(guestAPI)

  override def customer(token: String): Task[CustomerAPI] = {
    for {
      email <- authEngine.toEmail(token)
      _ <- customerRepo.byEmail(email).flatMap {
        case None    => Task.fail(new IllegalArgumentException(s"Email $email is not registered"))
        case Some(_) => Task.succeed(())
      }
    } yield customerAPI
  }
}
