package com.evolution
package repositories.algebras

import domain.Player.PlayerId
import domain.Room.RoomId
import domain.{Action, Card, Player, Room}

trait GameRepository[F[_]] {
  def addGameInProgress(room: Room): F[Unit]
  def addPlayerToLobby(player: Player, players: Player*): F[Unit]
  def getPlayersFromLobby: F[(Player, Player)]
  def getPlayersInRoom(roomId: RoomId): F[Option[(Player, Player)]]
  def getGameInProgress(roomId: RoomId): F[Option[Room]]
  def removeGameInProgress(roomId: RoomId): F[Boolean]
  def removePlayerAction(roomId: RoomId, playerId: PlayerId): F[Unit]
  def updateAction(roomId: RoomId, playerId: PlayerId, action: Action): F[Room]
  def updatePlayerCard(roomId: RoomId, playerId: PlayerId, card: Card): F[Unit]
  def updateRoomDeck(roomId: RoomId, renewed: List[Card]): F[Unit]
}
