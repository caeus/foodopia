package com.github.caeus.foodopia.storage

import doobie.util.transactor.Transactor
import zio.Task

class FoodopiaDB {}

object FoodopiaDB {
  def impl(transactor: Transactor[Task]): FoodopiaDB = new FoodopiaDB
}
