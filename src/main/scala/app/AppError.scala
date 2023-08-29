package app

import io.circe.{Json, JsonObject}

sealed trait AppError

object AppError {
  case object UserAlreadyExists extends AppError

  case object ServerError extends AppError

  def asString(error: AppError): String =
    error match {
      case UserAlreadyExists => s"api.users.duplicate"
      case ServerError => "api.users.server.error"
    }

  def asJson(error: AppError): Json =
    Json.fromJsonObject(JsonObject(
      "isError" -> Json.fromBoolean(true),
      "errorCode" -> Json.fromString(asString(error))
    ))
}
