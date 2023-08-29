package app.user.model

import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveEnumerationCodec

sealed trait Gender

object Gender {
  case object Male extends Gender

  case object Female extends Gender

  implicit val config: Configuration =
    Configuration.default.copy(transformConstructorNames = _.toLowerCase)
  implicit val codec: Codec[Gender] = deriveEnumerationCodec
}
