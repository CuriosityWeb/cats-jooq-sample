package app.user

import app.common.controller.AbstractController
import app.user.UserController.UsersPath
import app.user.model.User
import cats.effect.Async
import cats.implicits._
import org.http4s._
import org.http4s.circe.CirceEntityCodec._

object UserController {
  private val UsersPath = "users"
}

class UserController[F[_] : Async](service: UserService[F]) extends AbstractController[F] {

  val routes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case _@GET -> Root / UsersPath => service.getUsers.foldF(errorToResponse, Ok(_))
      case _@GET -> Root / UsersPath / email => service.getUserByEmail(email).foldF(errorToResponse, Ok(_))
      case _@GET -> Root / UsersPath / "stream" => Ok(service.streamAll)
      case req@POST -> Root / UsersPath =>
        req
          .as[User]
          .flatMap(user => service.addUser(user).foldF(errorToResponse, _ => Created(user)))
      case _@DELETE -> Root / UsersPath / email => service.deleteUser(email).foldF(errorToResponse, _ => NoContent())
      case _@PUT -> Root / UsersPath / email :? LastName(lastName) => service.updateLastName(email, lastName).foldF(errorToResponse, _ => NoContent())
    }

  private case object LastName extends OptionalQueryParamDecoderMatcher[String]("lastName")
}
