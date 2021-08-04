package com.evolution
package domain

import domain.Player.PlayerId
import domain.Room.RoomId

import io.circe.Encoder

sealed trait RoomState

object RoomState {
  final case class KeepAlive(value: Boolean) extends RoomState
}

final case class Room(
    roomId: RoomId,
    game: Game,
    playerOne: Player,
    playerTwo: Player,
    playerOneAction: Option[Action],
    playerTwoAction: Option[Action],
    deck: List[Card],
    cards: Map[PlayerId, Card]
)

object Room      {
  type RoomId          = String
  type GamesInProgress = Map[RoomId, Room]
}
