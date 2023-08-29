package app

import app.common.jooq.Jooq
import app.docs.DocsController
import app.user.repository.UserRepo
import app.user.{UserController, UserService}
import cats.effect.{IO, IOApp, Resource}
import cats.implicits._
import com.typesafe.config.{Config, ConfigFactory}
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.h2.{Driver => H2Driver}
import org.http4s.blaze.server.BlazeServerBuilder

import javax.sql.DataSource

object App extends IOApp.Simple {

  def run: IO[Unit] =
    loadConfig
      .flatMap(
        config =>
          dataSource(config)
            .evalTap(databaseMigration)
            .flatMap(initialize(config, _))
            .use(_ => IO.never)
      )

  private def initialize(config: Config, dataSource: DataSource) =
    for {
      _ <- Resource.pure(())

      jooq = new Jooq[IO](dataSource)

      userRepo = new UserRepo[IO](jooq)
      userService = new UserService[IO](userRepo)
      userController = new UserController[IO](userService)

      docsController = new DocsController[IO]

      httpApp = docsController
        .routes
        .combineK(userController.routes)
        .orNotFound

      server <- BlazeServerBuilder[IO]
        .bindHttp(config.getInt("http.port"), "0.0.0.0")
        .withHttpApp(httpApp)
        .resource
    } yield server

  private def loadConfig =
    IO.delay(ConfigFactory.load())

  private def dataSource(config: Config) =
    Resource.make[IO, HikariDataSource](
      IO.blocking(
        new HikariDataSource() {
          setDriverClassName(classOf[H2Driver].getName)
          setJdbcUrl(config.getString("database.url"))
          setUsername(config.getString("database.username"))
          setPassword(config.getString("database.password"))
          setConnectionInitSql("select 1")
        }
      )
    )(datasource => IO.blocking(datasource.close()))

  private def databaseMigration(dataSource: DataSource) =
    IO.blocking(
      Flyway
        .configure()
        .dataSource(dataSource)
        .baselineOnMigrate(true)
        .load()
        .migrate()
    )
}
