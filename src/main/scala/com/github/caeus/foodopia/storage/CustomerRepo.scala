package com.github.caeus.foodopia.storage

import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor
import zio.Task
import zio.interop.catz._
case class CustomerRow(name: String, email: String, hashpw: String)

trait CustomerRepo {

  def create(row: CustomerRow): Task[Unit]
  def byEmail(email: String): Task[Option[CustomerRow]]

}
object CustomerRepo {
  def impl(xa: Transactor[Task]): CustomerRepo = new DefaultCustomerRepo(xa)
}

final class DefaultCustomerRepo(xa: Transactor[Task]) extends CustomerRepo {
  private val map = scala.collection.mutable.Map.empty[String, CustomerRow]

  def _create(row: CustomerRow): ConnectionIO[Unit] = {

    sql"insert into customers(email, name, hashpw) values (${row.email},${row.name},${row.hashpw})".update.run
      .flatMap {
        case 1 => AsyncConnectionIO.pure(())
        case _ =>
          AsyncConnectionIO.raiseError(
            new IllegalArgumentException(s"Email ${row.email} is already registered"))
      }
  }
  override def create(row: CustomerRow): Task[Unit] = {
    _create(row).transact(xa)
  }

  def _byEmail(email: String): ConnectionIO[Option[CustomerRow]] = {

    sql"select c.email, c.name, c.hashpw from customers c where c.email = $email"
      .query[CustomerRow]
      .option
  }
  override def byEmail(email: String): Task[Option[CustomerRow]] =
    _byEmail(email).transact(xa)

}
