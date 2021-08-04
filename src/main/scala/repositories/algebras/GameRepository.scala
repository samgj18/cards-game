package com.evolution
package repositories.algebras

import domain.Room.RoomId
import domain.{Action, Player, Room}

trait GameRepository[F[_]] {
  def addGameInProgress(room: Room): F[Unit]
  def addPlayerToLobby(player: Player, players: Player*): F[Unit]
  def getPlayersFromLobby: F[(Player, Player)]
  def getPlayersInRoom(roomId: RoomId): F[Option[(Player, Player)]]
  def getGameInProgress(roomId: RoomId): F[Option[Room]]
  def removeGameInProgress(roomId: RoomId): F[Boolean]
  def updateActionAndGetRoom(roomId: RoomId, player: Player, action: Action): F[Room]
}
