package com.evolution
package domain

import domain.Player.{PlayerId, Token}

import io.circe.{Decoder, Encoder}

final case class Player(
    id: PlayerId,
    name: String
)

object Player {
  type PlayerId  = String
  type Token     = Int
  type Liquidity = Map[PlayerId, Token]

  implicit val decoder: Decoder[Player] = {
    Decoder.forProduct2("id", "name")((id, name) => Player(id, name))
  }

  implicit val encoder: Encoder[Player] = {
    Encoder.forProduct2("id", "name")(player => (player.id, player.name))
  }
}
