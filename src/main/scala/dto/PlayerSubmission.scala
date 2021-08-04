package com.evolution
package dto

import io.circe.{Decoder, Encoder}

object PlayerSubmission {
  implicit val decoder: Decoder[PlayerSubmission] = {
    Decoder.forProduct1("name")(name => PlayerSubmission(name))
  }

  implicit val encoder: Encoder[PlayerSubmission] = {
    Encoder.forProduct1("name")(submission => (submission.name))
  }
}
case class PlayerSubmission(name: String)
