package com.evolution
package domain

import io.circe.{Decoder, Encoder}

sealed trait Action

object Action {

  case object PlayCard extends Action
  case object Fold     extends Action

  implicit val decoder: Decoder[Action] = Decoder[String].emap {
    case "playCard" => Right(PlayCard)
    case "fold"     => Right(Fold)
    case other      => Left(s"Invalid mode: $other")
  }

  implicit val encoder: Encoder[Action] = Encoder[String].contramap {
    case PlayCard => "playCard"
    case Fold     => "fold"
  }
}
