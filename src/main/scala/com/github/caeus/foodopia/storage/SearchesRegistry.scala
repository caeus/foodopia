package com.github.caeus.foodopia.storage

import com.github.caeus.foodopia.model.{RestaurantsFilter, SearchRecord}
import zio.Task
import zio.clock.Clock

import scala.collection.mutable.ListBuffer

trait SearchesRegistry {

  def register(user: String, search: RestaurantsFilter): Task[Unit]
  def byUser(user: String): Task[Seq[SearchRecord]]
}
object SearchesRegistry {
  def impl(clock: Clock.Service[Any]): SearchesRegistry =
    new DefaultSearchesRegistry(clock: Clock.Service[Any])
}

final class DefaultSearchesRegistry(clock: Clock.Service[Any]) extends SearchesRegistry {

  private val registry = scala.collection.mutable.Map.empty[String, ListBuffer[SearchRecord]]

  override def register(user: String, search: RestaurantsFilter): Task[Unit] = {

    for {
      at <- clock.currentDateTime.map(_.toInstant)
      _ <- Task.effect(
        registry.getOrElseUpdate(user, ListBuffer.empty).addOne(SearchRecord(search, at)))
    } yield ()
  }

  override def byUser(user: String): Task[Seq[SearchRecord]] =
    Task.effect(registry.getOrElse(user, ListBuffer.empty).toSeq)
}
