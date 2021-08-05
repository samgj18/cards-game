package com.evolution
package domain

import domain.Player.{PlayerId, Players}
import domain.Room.{Actions, Cards, RoomId}

sealed trait RoomState

object RoomState {
  final case class KeepAlive(value: Boolean) extends RoomState
}

final case class Room(
    roomId: RoomId,
    game: Game,
    players: Players,
    playersActions: Actions,
    deck: List[Card],
    cards: Cards
)

object Room      {
  type RoomId          = String
  type GamesInProgress = Map[RoomId, Room]
  type Cards           = Map[PlayerId, Card]
  type Actions         = Map[PlayerId, Action]

}
