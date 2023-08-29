import com.typesafe.config.ConfigFactory

import scala.util.Try

lazy val H2Version = "2.2.220"
lazy val config = ConfigFactory.parseFile(new File("src/main/resources/application.conf"))
val Http4sVersion = "0.23.23"
val Http4sBlazeVersion = "0.23.15"
val CirceVersion = "0.14.5"

lazy val root =
  (project in file("."))
    .enablePlugins(JooqCodegenPlugin)
    .settings(
      name := "cats-jooq",
      scalaVersion := "2.13.11",
      version := "0.1.0-SNAPSHOT"
    )
    .settings(
      jooqVersion := "3.18.6",
      autoJooqLibrary := false,
      jooqCodegenConfig := file("jooq/jooq-codegen.xml"),
      jooqCodegenMode := CodegenMode.Unmanaged,
      jooqCodegenVariables ++= Map(
        "DB_JDBC_URL" -> Try(config.getString("database.url")).toOption.getOrElse(""),
        "DB_USER" -> Try(config.getString("database.username")).toOption.getOrElse(""),
        "DB_PASSWORD" -> Try(config.getString("database.password")).toOption.getOrElse(""),
      )
    )
    .settings(
      libraryDependencies ++= Seq(
        "org.jooq" % "jooq" % jooqVersion.value,
        "org.jooq" %% "jooq-scala" % jooqVersion.value,
        "org.jooq" % "jooq-codegen" % jooqVersion.value % JooqCodegen,
        "org.jooq" % "jooq-meta-extensions" % jooqVersion.value
      ),
      libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-core" % "2.10.0",
        "org.typelevel" %% "log4cats-slf4j" % "2.6.0",
        "org.typelevel" %% "cats-effect" % "3.5.1",
      ),
      libraryDependencies ++= Seq(
        "org.http4s" %% "http4s-blaze-server" % Http4sBlazeVersion,
        "org.http4s" %% "http4s-blaze-client" % Http4sBlazeVersion,
        "org.http4s" %% "http4s-circe" % Http4sVersion,
        "org.http4s" %% "http4s-dsl" % Http4sVersion,
      ),
      libraryDependencies ++= Seq(
        "io.circe" %% "circe-core" % CirceVersion,
        "io.circe" %% "circe-generic" % CirceVersion,
        "io.circe" %% "circe-parser" % CirceVersion,
        "io.circe" %% "circe-generic-extras" % "0.14.3"
      ),
      libraryDependencies ++= Seq(
        "com.h2database" % "h2" % H2Version,
        "com.h2database" % "h2" % H2Version % JooqCodegen
      ),
      libraryDependencies ++= Seq(
        "com.zaxxer" % "HikariCP" % "5.0.1",
        "org.flywaydb" % "flyway-core" % "9.21.2",
        "com.typesafe" % "config" % "1.4.2",
        "ch.qos.logback" % "logback-classic" % "1.4.11"
      )
    )
    .settings(
      scalacOptions ++= Seq("-Ymacro-annotations")
    )
