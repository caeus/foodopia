package com.github.caeus.foodopia.util
import cats.effect.Async
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.util.Meta
import io.circe.generic.AutoDerivation
import io.circe.jawn.parse
import io.circe.{Decoder, Encoder, Json}
import org.postgresql.util.PGobject

case class AsJson[T](value: T)

trait FoodbatonCodecs extends AutoDerivation {
  implicit class IntConnectionIOOps(private val value: ConnectionIO[Int]) {
    def failIfZero(msg: => String): ConnectionIO[Unit] = value.flatMap {
      case 0 => Async[ConnectionIO].raiseError[Unit](new IllegalStateException(msg))
      case _ => ().pure[ConnectionIO]
    }

    def failIfNot1(msg: => String): ConnectionIO[Unit] = value.flatMap {
      case 1 => ().pure[ConnectionIO]
      case _ =>
        Async[ConnectionIO].raiseError[Unit](new IllegalStateException(msg))
    }
  }

  implicit val jsonMeta: Meta[Json] =
    Meta.Advanced
      .other[PGobject]("jsonb")
      .timap[Json](a => parse(a.getValue).leftMap[Json](e => throw e).merge)(
        a => {
          val o = new PGobject
          o.setType("jsonb")
          o.setValue(a.noSpaces)
          o
        }
      )

  implicit def jsonValueMeta[T: Decoder: Encoder]: Meta[AsJson[T]] =
    jsonMeta.imap { json =>
      json.as[T].fold(throw _, AsJson.apply)
    } { t: AsJson[T] =>
      Encoder[T].apply(t.value)
    }
}
