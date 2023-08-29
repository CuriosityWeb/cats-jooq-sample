package app.user.repository

import app.common.jooq.Jooq
import app.user.model.{Gender, User}
import app.user.repository.UserRepo._
import cats.data.EitherT
import cats.effect.Async
import fs2.Stream
import generated.jooq.enums.UsersGender
import generated.jooq.tables.Users.USERS
import generated.jooq.tables.records.UsersRecord
import org.jooq.DSLContext
import org.typelevel.log4cats.slf4j.Slf4jLogger

object UserRepo {

  private def insertUserQuery(user: User)(ctx: DSLContext) =
    ctx
      .insertInto(USERS)
      .set(record(user))

  private def getUsersQuery(ctx: DSLContext) =
    ctx
      .selectFrom(USERS)

  private def getUserByEmailQuery(email: String)(ctx: DSLContext) =
    ctx
      .selectFrom(USERS)
      .where(USERS.EMAIL.eq(email))

  private def deleteQuery(email: String)(ctx: DSLContext) =
    ctx
      .deleteFrom(USERS)
      .where(USERS.EMAIL.eq(email))

  private def updateLastNameQuery(email: String, lastName: Option[String])(ctx: DSLContext) =
    ctx
      .update(USERS)
      .set(USERS.LAST_NAME, lastName.orNull)
      .where(USERS.EMAIL.eq(email))

  private def mapper(record: UsersRecord) =
    User(
      firstName = record.getFirstName,
      lastName = Option(record.getLastName),
      gender = record.getGender match {
        case UsersGender.Male => Gender.Male
        case UsersGender.Female => Gender.Female
      },
      dob = record.getDob,
      email = record.getEmail,
      mobile = Option(record.getEmail)
    )

  private def record(user: User) =
    new UsersRecord(
      firstName = user.firstName,
      lastName = user.lastName.orNull,
      gender = user.gender match {
        case Gender.Male => UsersGender.Male
        case Gender.Female => UsersGender.Female
      },
      dob = user.dob,
      email = user.email,
      mobile = user.mobile.orNull
    )
}

class UserRepo[F[_] : Async](jooq: Jooq[F]) {
  private val log = Slf4jLogger.getLogger[F]

  private val table = USERS

  def addUser(user: User): EitherT[F, Throwable, Int] = jooq.insert(insertUserQuery(user))
  def getUsers: EitherT[F, Throwable, List[User]] = jooq.fetchAndMap(getUsersQuery)(mapper)
  def getUserByEmail(email: String): EitherT[F, Throwable, Option[User]] = jooq.fetchAndMap(getUserByEmailQuery(email))(mapper).map(_.headOption)
  def deleteUser(email: String): EitherT[F, Throwable, Int] = jooq.delete(deleteQuery(email))
  def updateLastName(email: String, lastName: Option[String]): EitherT[F, Throwable, Int] = jooq.update(updateLastNameQuery(email, lastName))

  def stream: Stream[F, User] =
    jooq
      .fetchStream(_.fetchStream(table))
      .map(mapper)

}
