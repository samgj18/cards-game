package com.evolution
package infrastructure.services

import domain.Game.singleCardGame
import domain.Message._
import domain.Participant._
import domain.Player.PlayerId
import domain.Result._
import domain.Room.RoomId
import domain.RoomState._
import domain._
import repositories.algebras._

import cats.effect._
import cats.implicits._

trait ActionManager[F[_]] {
  def performAction(roomId: RoomId, playerId: PlayerId, action: Action): F[Result]
  def matchResolver(result: Result): F[KeepAlive]
}

object ActionService         {
  def make[F[_]: Sync](
      gameRepository: GameRepository[F],
      playerRepository: PlayerRepository[F],
      messageRepository: MessageRepository[F]
  ): F[ActionService[F]] = {
    Sync[F].delay(new ActionService[F](gameRepository, playerRepository, messageRepository))
  }
}

class ActionService[F[_]: Sync](
    gameRepository: GameRepository[F],
    playerRepository: PlayerRepository[F],
    messageRepository: MessageRepository[F]
) extends ActionManager[F] {

  def performAction(roomId: RoomId, playerId: PlayerId, action: Action): F[Result] = {
    gameRepository.getGameInProgress(roomId).flatMap {
      case Some(_) =>
        gameRepository
          .updateActionAndGetRoom(roomId, playerId, action)
          .flatMap { newRoom =>
            Sync[F].delay(singleCardGame(newRoom.playersActions, newRoom.players, playerId))
          }
      case None    => Sync[F].delay(NotStarted)
    }
  }

  def matchResolver(result: Result): F[KeepAlive]                                  = {
    result match {
      case Finished(one, two)         =>
        changeStatus(one) >> changeStatus(two) as KeepAlive(false)
      case InProgress(turn)           =>
        messageRepository.addMessageToNotifications(turn.id, Hurry) as KeepAlive(true)
      case Draw(playerOne, playerTwo) =>
        messageRepository.addMessageToNotifications(playerOne.id, Hurry) >>
          messageRepository.addMessageToNotifications(playerTwo.id, Hurry) as KeepAlive(true)
      case NotStarted                 => Sync[F].delay(KeepAlive(false))
    }
  }

  private def changeStatus(player: Participant): F[Unit]                           = {
    player match {
      case Winner(participant, revenue) =>
        playerRepository.updateLiquidity(Winner(participant, revenue)) >> messageRepository
          .addMessageToNotifications(participant.id, Win) >> notifyLiquidity(participant.id)
      case Loser(participant, lost)     =>
        playerRepository.updateLiquidity(Loser(participant, lost)) >> messageRepository
          .addMessageToNotifications(participant.id, Lost) >> notifyLiquidity(participant.id)
    }
  }

  def notifyLiquidity(playerId: PlayerId): F[Unit]                                 = {
    playerRepository
      .getCurrentLiquidity(playerId)
      .flatMap(token => {
        messageRepository
          .addMessageToNotifications(playerId, Balance(playerId, token))
      })
  }
}
