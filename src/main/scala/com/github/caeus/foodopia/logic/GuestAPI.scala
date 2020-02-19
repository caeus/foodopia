package com.github.caeus.foodopia.logic

import com.github.caeus.foodopia.middleware.AuthEngine
import com.github.caeus.foodopia.storage.{CustomerRepo, CustomerRow}
import com.github.caeus.foodopia.model.{LogInReq, LogInResp, SignUpReq, SignUpResp}
import zio.Task

trait GuestAPI {
  def logIn(req: LogInReq): Task[LogInResp]
  def signUp(req: SignUpReq): Task[SignUpResp]
}
object GuestAPI {
  def impl(customerRepo: CustomerRepo, authEngine: AuthEngine): GuestAPI =
    new DefaultGuestAPI(customerRepo: CustomerRepo, authEngine: AuthEngine)
}
final class DefaultGuestAPI(customerRepo: CustomerRepo, authEngine: AuthEngine) extends GuestAPI {

  override def logIn(req: LogInReq): Task[LogInResp] = {
    for {
      _ <- customerRepo.byEmail(req.email).flatMap {
        case None =>
          Task.fail(new IllegalArgumentException(s"Email ${req.email} is not registered"))
        case Some(row) => Task.succeed(row)
      }
      token <- authEngine.toToken(req.email)
    } yield LogInResp(token)
  }

  override def signUp(req: SignUpReq): Task[SignUpResp] = {
    for {
      _ <- customerRepo
        .byEmail(req.email)
        .flatMap {
          case Some(_) =>
            Task.fail(new IllegalArgumentException(s"Email ${req.email} is already registered"))
          case None => Task.succeed(())
        }
      hashpw <- authEngine.hashPw(req.password)
      _      <- customerRepo.create(CustomerRow(req.name, req.email, hashpw))
      token  <- authEngine.toToken(req.email)
    } yield SignUpResp(token)
  }
}