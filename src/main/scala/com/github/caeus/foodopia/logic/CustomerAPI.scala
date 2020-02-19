package com.github.caeus.foodopia.logic

import com.github.caeus.foodopia.middleware.GeoCitiesDB
import com.github.caeus.foodopia.model.{Restaurant, RestaurantsFilter, SearchRecord}
import com.github.caeus.foodopia.storage.SearchesRegistry
import zio.Task

trait CustomerAPI {
  def searches: Task[Seq[SearchRecord]]

  def restaurantsBy(filter: RestaurantsFilter): Task[Seq[Restaurant]]
  def logOut: Task[Unit]
}
object CustomerAPI {
  def impl(email: String,
           restaurantsEngine: RestaurantsEngine,
           searchesRegistry: SearchesRegistry,
           geoCitiesDB: GeoCitiesDB): CustomerAPI =
    new DefaultCustomerAPI(email,
                           restaurantsEngine,
                           searchesRegistry: SearchesRegistry,
                           geoCitiesDB: GeoCitiesDB)
}
final class DefaultCustomerAPI(email: String,
                               restaurantsEngine: RestaurantsEngine,
                               searchesRegistry: SearchesRegistry,
                               geoCitiesDB: GeoCitiesDB)
    extends CustomerAPI {

  override def restaurantsBy(filter: RestaurantsFilter): Task[Seq[Restaurant]] = {
    for {
      _      <- searchesRegistry.register(email, filter)
      result <- restaurantsEngine.byFilter(filter)
    } yield result
  }

  override def logOut: Task[Unit] = Task.succeed(())

  override def searches: Task[Seq[SearchRecord]] = searchesRegistry.byUser(email)
}
