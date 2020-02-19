package com.github.caeus.foodopia.storage

import cats.effect.{Blocker, IO}
import com.github.caeus.foodopia.conf.DBConnectionConfig
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.flywaydb.core.Flyway
import zio._
import zio.interop.catz._

object BootstrapDB {
  def migrate(config: DBConnectionConfig): Task[Int] = {
    Task
      .effect(
        Flyway
          .configure()
          .dataSource(config.jdbcUrl, config.user, config.password)
          .load()
          .migrate())
  }
  def transactor(config: DBConnectionConfig)(
      implicit runtime: zio.Runtime[Any]): TaskManaged[HikariTransactor[Task]] = {
    for {
      ce <- ExecutionContexts.fixedThreadPool[Task](32).toManaged
      be <- Blocker[IO].toManaged
      xa <- HikariTransactor
        .newHikariTransactor[Task](
          driverClassName = "org.postgresql.Driver", // driver classname
          url = config.jdbcUrl, // connect URL
          user = config.user, // username
          pass = config.password, // password
          connectEC = ce, // await connection here
          blocker = be // execute JDBC operations here
        )
        .toManaged
    } yield xa
  }

  def prod(config: DBConnectionConfig)(
      implicit runtime: zio.Runtime[Any]): TaskManaged[HikariTransactor[Task]] = {
    for {
      _                          <- migrate(config).toManaged_
      xa: HikariTransactor[Task] <- transactor(config)
    } yield xa
  }
}
