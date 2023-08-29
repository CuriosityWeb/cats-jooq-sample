package app.docs

import app.AppError
import app.common.controller.AbstractController
import cats.Applicative
import cats.data.EitherT
import cats.effect.{Async, Resource}
import cats.implicits._
import io.circe.parser._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.{HttpRoutes, StaticFile}

import scala.io.Source

class DocsController[F[_] : Async] extends AbstractController[F] {

  override val routes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req@GET -> Root =>
        StaticFile
          .fromResource("swagger/dist/index.html", Some(req))
          .getOrElseF(NotFound())

      case _@GET -> Root / "docs" =>
        read
          .leftSemiflatMap[AppError](error =>
            log.error(error)("Failed parsing docs.json file")
              .map(_ => AppError.ServerError)
          ).foldF(errorToResponse, Ok(_))

      case req@GET -> Root / "swagger" / "assets" / path =>
        StaticFile
          .fromResource(s"swagger/dist/$path", Some(req))
          .getOrElseF(NotFound())
    }

  private def resource =
    Resource
      .make[F, Source](
        Async[F].blocking(Source.fromResource("swagger/docs.json"))
      )(source => Async[F].blocking(source.close()))

  private def read =
    EitherT(
      resource
        .use(source => Applicative[F].pure(source.mkString))
        .map(parse)
    )
}
