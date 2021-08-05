package com.evolution
package repositories.interpreters

import domain.Player.PlayerId
import domain.Room.{GamesInProgress, RoomId}
import domain.{Action, Player, Room}
import repositories.algebras.GameRepository

import cats.effect.std.Queue
import cats.effect.{Async, Ref, Sync}
import cats.implicits._

sealed trait Operation

object Operation              {
  case object Add      extends Operation
  case object Subtract extends Operation
}

object InMemoryGameRepository {
  def make[F[_]: Async]: F[GameRepository[F]] = {
    for {
      lobby           <- Queue.unbounded[F, Player]
      gamesInProgress <- Ref.of[F, GamesInProgress](Map.empty)
    } yield new InMemoryGameRepository[F](lobby, gamesInProgress)
  }
}

class InMemoryGameRepository[F[_]: Async](
    private val lobby: Queue[F, Player],
    private val gamesInProgress: Ref[F, GamesInProgress]
) extends GameRepository[F] {

  def addGameInProgress(room: Room): F[Unit]                      = {
    gamesInProgress.update(games => games + (room.roomId -> room))
  }

  def addPlayerToLobby(player: Player, players: Player*): F[Unit] = {
    players.traverse(current => lobby.offer(current)) >> lobby.offer(player)
  }

  def removeGameInProgress(roomId: RoomId): F[Boolean] = {
    gamesInProgress.modify { games =>
      games.get(roomId) match {
        case Some(_) => (games - roomId, true)
        case None    => (games, false)
      }
    }
  }

  def getPlayersFromLobby: F[(Player, Player)]         = {
    lobby.take.flatMap(playerOne => lobby.take.map(playerTwo => (playerOne, playerTwo)))
  }

  def getPlayersInRoom(roomId: RoomId): F[Option[(Player, Player)]] = {
    gamesInProgress.get.map(_.get(roomId) match {
      case Some(value) => Some(value.players.head._2, value.players.last._2)
      case None        => None
    })
  }

  def getGameInProgress(roomId: RoomId): F[Option[Room]]            = {
    gamesInProgress.get.map(_.get(roomId))
  }

  def updateActionAndGetRoom(roomId: RoomId, playerId: PlayerId, action: Action): F[Room] = {
    gamesInProgress
      .updateAndGet { games =>
        games.get(roomId) match {
          case Some(room) =>
            val newRoom = room.copy(playersActions = room.playersActions.get(playerId) match {
              case Some(value) => room.playersActions + (playerId -> value)
              case None        => room.playersActions + (playerId -> action)
            })

            games + (roomId -> newRoom)
          case None       => games
        }
      }
      .flatMap(games => Sync[F].delay(games(roomId)))
  }

}
