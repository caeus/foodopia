package com.github.caeus.foodopia.logic

import com.github.caeus.foodopia.middleware.{AuthEngine, GeoCitiesDB}
import com.github.caeus.foodopia.storage.{CustomerRepo, SearchesRegistry}
import zio.Task

trait FoodopiaAPI {
  def guest: Task[GuestAPI]
  def customer(token: String): Task[CustomerAPI]
}

object FoodopiaAPI {
  def impl(customerRepo: CustomerRepo,
           authEngine: AuthEngine,
           restaurantsEngine: RestaurantsEngine,
           searchesRegistry: SearchesRegistry,
           geoCitiesDB: GeoCitiesDB): FoodopiaAPI =
    new DefaultFoodopiaAPI(customerRepo: CustomerRepo,
                           authEngine: AuthEngine,
                           restaurantsEngine: RestaurantsEngine,
                           searchesRegistry: SearchesRegistry,
                           geoCitiesDB: GeoCitiesDB)
}
final class DefaultFoodopiaAPI(customerRepo: CustomerRepo,
                               authEngine: AuthEngine,
                               restaurantsEngine: RestaurantsEngine,
                               searchesRegistry: SearchesRegistry,
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
    } yield CustomerAPI.impl(email, restaurantsEngine, searchesRegistry, geoCitiesDB)
  }
}
