package app.common.controller

import app.AppError
import app.AppError.{ServerError, UserAlreadyExists}
import cats.effect.Async
import org.http4s.{HttpRoutes, Response}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

abstract class AbstractController[F[_] : Async] extends Http4sDsl[F] {

  protected val log: Logger[F] = Slf4jLogger.getLogger[F]

  val routes: HttpRoutes[F]

  protected final def errorToResponse(er: AppError): F[Response[F]] =
    er match {
      case UserAlreadyExists => BadRequest(AppError.asJson(er))
      case ServerError => InternalServerError(AppError.asJson(er))
    }
}
