package com.github.caeus.foodopia.storage

import zio.Task

case class CustomerRow(name: String, email: String, hashpw: String)

trait CustomerRepo {

  def create(row: CustomerRow): Task[Unit]
  def byEmail(email: String): Task[Option[CustomerRow]]

}
object CustomerRepo {
  def impl: CustomerRepo = new DefaultCustomerRepo()
}

final class DefaultCustomerRepo extends CustomerRepo {
  private val asd = scala.collection.mutable.Map.empty[String, CustomerRow]

  override def create(row: CustomerRow): Task[Unit] = Task.effect {
    if (asd.contains(row.email)) throw new IllegalStateException("WHAAAA")
    asd.update(row.email, row)
  }

  override def byEmail(email: String): Task[Option[CustomerRow]] = Task.effect {
    asd.get(email)
  }
}
