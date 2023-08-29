package app.user

import app.AppError
import app.user.model.User
import app.user.repository.UserRepo
import cats.Applicative
import cats.data.EitherT
import cats.effect.kernel.Sync
import cats.implicits._
import fs2.Stream
import org.jooq.exception.IntegrityConstraintViolationException
import org.typelevel.log4cats.slf4j.Slf4jLogger

class UserService[F[_]: Sync](repo: UserRepo[F]) {

  private val log = Slf4jLogger.getLogger[F]

  def streamAll: Stream[F, User] = repo.stream
  def addUser(user: User): EitherT[F, AppError, Unit] = processResult(repo.addUser(user)).void
  def getUsers: EitherT[F, AppError, List[User]] = processResult(repo.getUsers)
  def getUserByEmail(email: String): EitherT[F, AppError, Option[User]] = processResult(repo.getUserByEmail(email))
  def deleteUser(email: String): EitherT[F, AppError, Unit] = processResult(repo.deleteUser(email)).void
  def updateLastName(email: String, lastName: Option[String]): EitherT[F, AppError, Unit] = processResult(repo.updateLastName(email, lastName)).void

  private def processResult[T](res: EitherT[F, Throwable, T]) =
    res
      .leftSemiflatMap[AppError] {
        case _: IntegrityConstraintViolationException => Applicative[F].pure(AppError.UserAlreadyExists)
        case error =>
          log.error(error)("server-error")
            .map(_ => AppError.ServerError)
      }
}
