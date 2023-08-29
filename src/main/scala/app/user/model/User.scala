package app.user.model

import io.circe.generic.JsonCodec

import java.time.LocalDate

@JsonCodec
case class User(firstName: String,
                lastName: Option[String],
                gender: Gender,
                dob: LocalDate,
                email: String,
                mobile: Option[String])
