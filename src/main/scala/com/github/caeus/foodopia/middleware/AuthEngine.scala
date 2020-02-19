package com.github.caeus.foodopia.middleware

import com.github.caeus.foodopia.conf.FoodopiaAuth
import org.mindrot.jbcrypt.BCrypt
import org.reactormonk.{CryptoBits, PrivateKey}
import zio.Task
import zio.clock.Clock

trait AuthEngine {

  def hashPw(password: String): Task[String]
  def verify(password: String, hashpw: String): Task[Boolean]
  def toToken(email: String): Task[String]
  def toEmail(token: String): Task[String]
}
object AuthEngine {
  def impl(clock: Clock.Service[Any], conf: FoodopiaAuth): AuthEngine = new DefaultAuthEngine(clock, conf: FoodopiaAuth)
}
final class DefaultAuthEngine(clock: Clock.Service[Any], conf: FoodopiaAuth) extends AuthEngine {

  val crypto = CryptoBits(PrivateKey(scala.io.Codec.toUTF8(conf.privateKey)))

  override def toToken(email: String): Task[String] =
    for {
      now           <- clock.currentDateTime.map(_.toInstant.toEpochMilli)
      token: String <- Task.effect(crypto.signToken(email, now.toString))
    } yield token

  override def toEmail(token: String): Task[String] =
    for {
      email <- Task
        .effect(crypto.validateSignedToken(token))
        .flatMap {
          case Some(email) => Task.succeed(email)
          case None        => Task.fail(new IllegalArgumentException("Invalid token"))
        }
    } yield email

  override def hashPw(password: String): Task[String] =
    for {
      salt   <- Task.effect(BCrypt.gensalt())
      hashpw <- Task.effect(BCrypt.hashpw(password, salt))
    } yield hashpw

  override def verify(password: String, hashpw: String): Task[Boolean] =
    Task.effect(BCrypt.checkpw(password, hashpw))
}
