package com.evolution
package infrastructure.services

import domain.Game.game
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
      case Some(room) =>
        actions(room) match {
          case (None, None)                                   =>
            if (comparator(playerId, room.playerOne))
              gameRepository
                .updateActionAndGetRoom(roomId, room.playerOne, action) as
                game(Some(action), None, room.playerOne, room.playerTwo)
            else
              gameRepository
                .updateActionAndGetRoom(roomId, room.playerOne, action) as
                game(None, Some(action), room.playerOne, room.playerTwo)
          case (Some(playerOneAction), None)                  =>
            gameRepository
              .updateActionAndGetRoom(roomId, room.playerTwo, action) as
              game(Some(playerOneAction), Some(action), room.playerOne, room.playerTwo)
          case (None, Some(playerTwoAction))                  =>
            gameRepository
              .updateActionAndGetRoom(roomId, room.playerTwo, action) as
              game(Some(action), Some(playerTwoAction), room.playerOne, room.playerTwo)
          case (Some(playerOneAction), Some(playerTwoAction)) =>
            Sync[F].delay(
              game(Some(playerOneAction), Some(playerTwoAction), room.playerOne, room.playerTwo)
            )
        }
      case None       => Sync[F].delay(NotStarted)
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

  private def comparator(playerId: PlayerId, player: Player): Boolean              =
    player.id == playerId

  private def actions(room: Room): (Option[Action], Option[Action]) =
    (room.playerOneAction, room.playerTwoAction)

  private def changeStatus(player: Participant): F[Unit] = {
    player match {
      case Winner(player, revenue) =>
        playerRepository.updateLiquidity(Winner(player, revenue)) >> messageRepository
          .addMessageToNotifications(player.id, Win)
      case Loser(player, lost)     =>
        playerRepository.updateLiquidity(Loser(player, lost)) >> messageRepository
          .addMessageToNotifications(player.id, Lost)
    }
  }
}
