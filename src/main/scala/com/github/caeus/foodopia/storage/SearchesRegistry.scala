package com.github.caeus.foodopia.storage

import java.time.Instant
import java.util.UUID

import com.github.caeus.foodopia.model.{RestaurantsFilter, SearchRecord}
import com.github.caeus.foodopia.util.{AsJson, FoodbatonCodecs}
import doobie._
import cats.implicits._
import cats._
import doobie.implicits._
import doobie.util.transactor.Transactor
import zio.Task
import zio.clock.Clock
import zio.interop.catz._

import scala.collection.mutable.ListBuffer

trait SearchesRegistry {

  def register(user: String, search: RestaurantsFilter): Task[Unit]
  def byUser(user: String): Task[Seq[SearchRecord]]
}
object SearchesRegistry {
  def impl(xa: Transactor[Task], clock: Clock.Service[Any]): SearchesRegistry =
    new DefaultSearchesRegistry(xa: Transactor[Task], clock: Clock.Service[Any])
}

final class DefaultSearchesRegistry(xa: Transactor[Task], clock: Clock.Service[Any])
  extends SearchesRegistry
    with FoodbatonCodecs {

  private val registry = scala.collection.mutable.Map.empty[String, ListBuffer[SearchRecord]]
  def _register(id: String,
                at: Instant,
                user: String,
                search: RestaurantsFilter): ConnectionIO[Unit] = {
    sql"insert into searches(id,data,at,by) values($id,${AsJson(search)},$at, $user)".update.run
      .failIfNot1("Failed to insert row")
  }
  override def register(user: String, search: RestaurantsFilter): Task[Unit] = {
    for {
      at <- clock.currentDateTime.map(_.toInstant)
      id <- Task.effect(UUID.randomUUID().toString)
      _  <- _register(id, at, user, search).transact(xa)
    } yield ()
  }

  def _byUser(user: String): ConnectionIO[Seq[SearchRecord]] ={
    sql"select s.data, s.at from searches s where s.by = $user order by s.at desc"
      .query[(AsJson[RestaurantsFilter],Instant)]
      .map(t => SearchRecord(t._1.value,t._2)  )
      .to[Seq]
  }

  override def byUser(user: String): Task[Seq[SearchRecord]] =
    _byUser(user).transact(xa)
}
