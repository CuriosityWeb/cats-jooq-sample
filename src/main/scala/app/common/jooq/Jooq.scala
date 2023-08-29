package app.common.jooq

import app.common.jooq.Jooq.Dialect
import cats.Applicative
import cats.data.EitherT
import cats.effect.{Async, Resource}
import cats.implicits._
import fs2.Stream
import org.jooq.impl.DSL
import org.jooq.{DSLContext, Delete, Insert, Record, Result, SQLDialect, Select, Update}

import java.sql.Connection
import java.util.concurrent.CompletionStage
import java.util.stream.{Stream => JStream}
import javax.sql.DataSource
import scala.jdk.CollectionConverters._
import scala.util.Try

object Jooq {
  private val Dialect = SQLDialect.H2
}

class Jooq[F[_] : Async](dataSource: DataSource) {

  def fetch[R <: Record](block: DSLContext => Select[R]): EitherT[F, Throwable, List[R]] =
    execute[Select[R], Result[R]](block, _.fetchAsync()).map(_.asScala.toList)

  def fetchAndMap[R <: Record, O](block: DSLContext => Select[R])(mapper: R => O): EitherT[F, Throwable, List[O]] =
    execute[Select[R], Result[R]](block, _.fetchAsync()).map(_.asScala.map(mapper).toList)

  def fetchAsync[R <: Record](block: DSLContext => CompletionStage[Result[R]]): EitherT[F, Throwable, List[R]] =
    execute[CompletionStage[Result[R]], Result[R]](block, identity).map(_.asScala.toList)

  def fetchAsyncAndMap[R <: Record, O](block: DSLContext => CompletionStage[Result[R]])(mapper: R => O): EitherT[F, Throwable, List[O]] =
    execute[CompletionStage[Result[R]], Result[R]](block, identity).map(_.asScala.map(mapper).toList)

  def insert[R <: Record](block: DSLContext => Insert[R]): EitherT[F, Throwable, Int] =
    execute[Insert[R], Integer](block, _.executeAsync()).map(_.intValue())

  def delete[R <: Record](block: DSLContext => Delete[R]): EitherT[F, Throwable, Int] =
    execute[Delete[R], Integer](block, _.executeAsync()).map(_.intValue())

  def update[R <: Record](block: DSLContext => Update[R]): EitherT[F, Throwable, Int] =
    execute[Update[R], Integer](block, _.executeAsync()).map(_.intValue())

  def fetchStream[R](sql: DSLContext => JStream[R]): Stream[F, R] =
    Stream.resource(
        connection
          .map(DSL.using(_, Dialect))
      )
      .map(sql(_))
      .map(_.iterator().asScala)
      .flatMap(Stream.fromIterator[F](_, 10))

  private def dslContext =
    connection
      .map(DSL.using(_, Dialect))

  private def connection =
    Resource.make[F, Connection](
      Applicative[F].pure(dataSource.getConnection)
    )(conn => Applicative[F].pure(()).map(_ => conn.close()))

  private def execute[OP, Result](block: DSLContext => OP, fxn: OP => CompletionStage[Result]) =
    EitherT(
      dslContext
        .use(ctx =>
          fromTry(Try(block(ctx)))
            .map(fxn)
            .flatMap(fromCompletionStage)
            .value
        )
    )

  private def fromTry[T](tried: Try[T]) =
    EitherT.fromEither[F](tried.toEither)

  private def fromCompletionStage[T](cs: CompletionStage[T]) =
    EitherT(
      Async[F]
        .fromCompletionStage(Async[F].delay(cs))
        .map[Either[Throwable, T]](Right(_))
        .handleError(Left(_))
    )
}
