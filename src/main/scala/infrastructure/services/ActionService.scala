package com.evolution
package infrastructure.services

import domain.Game.singleCardGame
import domain.Message._
import domain.Participant._
import domain.Player.PlayerId
import domain.Result._
import domain.Room.{Cards, RoomId}
import domain.RoomState._
import domain._
import repositories.algebras._

import cats.effect._
import cats.implicits._

trait ActionManager[F[_]] {
  def performer(roomId: RoomId, playerId: PlayerId, action: Action): F[Result]
  def decider(result: Result): F[KeepAlive]
}

object ActionService         {
  def make[F[_]: Sync](
      gameRepository: GameRepository[F],
      playerRepository: PlayerRepository[F],
      messageRepository: MessageRepository[F],
      roomService: RoomService[F]
  ): F[ActionService[F]] = {
    Sync[F].delay(
      new ActionService[F](gameRepository, playerRepository, messageRepository, roomService)
    )
  }
}

class ActionService[F[_]: Sync](
    gameRepository: GameRepository[F],
    playerRepository: PlayerRepository[F],
    messageRepository: MessageRepository[F],
    roomService: RoomService[F]
) extends ActionManager[F] {

  /**
    * Perform action will take the input from the user and return a result,
    * the result could be either InProgress, Finished, Draw and NotStarted depending of the current situation of the game
    */
  def performer(roomId: RoomId, playerId: PlayerId, action: Action): F[Result] = {
    gameRepository.getGameInProgress(roomId).flatMap {
      case Some(_) =>
        gameRepository
          .updateAction(roomId, playerId, action)
          .flatMap { newRoom =>
            Sync[F]
              .delay(singleCardGame(newRoom.playersActions, newRoom.players, playerId, newRoom))
          }
      case None    => Sync[F].delay(NotStarted)
    }
  }

  /**
    * The decider will check for the current result of the game and will determine whether if
    * the game has the right to exist or is it already over.
    */
  def decider(result: Result): F[KeepAlive]                                    = {
    result match {
      case Finished(one, two)               =>
        changeStatus(one) >> changeStatus(two) as KeepAlive(false)
      case InProgress(turn)                 =>
        messageRepository.addNotification(turn.id, Hurry) as KeepAlive(true)
      case Draw(room, playerOne, playerTwo) =>
        roomService.dispatcher(room.deck, playerOne.id, playerTwo.id).flatMap {
          case (cards, deck) =>
            resolver(room.roomId, playerOne.id, playerTwo.id, cards, deck) as KeepAlive(true)
        }
      case NotStarted                       => Sync[F].delay(KeepAlive(false))
    }
  }

  private def changeStatus(player: Participant): F[Unit]                       = {
    player match {
      case Winner(participant, revenue) =>
        playerRepository.updateLiquidity(Winner(participant, revenue)) >> messageRepository
          .addNotification(participant.id, Win) >> notifyLiquidity(participant.id)
      case Loser(participant, lost)     =>
        playerRepository.updateLiquidity(Loser(participant, lost)) >> messageRepository
          .addNotification(participant.id, Lost) >> notifyLiquidity(participant.id)
    }
  }

  private def notifyLiquidity(playerId: PlayerId): F[Unit]                     = {
    playerRepository
      .getCurrentLiquidity(playerId)
      .flatMap(token => {
        messageRepository
          .addNotification(playerId, Balance(playerId, token))
      })
  }

  private def resolver(
      roomId: RoomId,
      playerOneId: PlayerId,
      playerTwoId: PlayerId,
      cards: Cards,
      deck: List[Card]
  ): F[Unit] = {
    gameRepository.removePlayerAction(roomId, playerOneId) >>
      gameRepository.removePlayerAction(roomId, playerTwoId) >>
      gameRepository.updatePlayerCard(roomId, playerOneId, cards(playerOneId)) >>
      gameRepository.updatePlayerCard(roomId, playerTwoId, cards(playerTwoId)) >>
      messageRepository.addNotification(playerOneId, RoomACK(roomId, cards(playerOneId).value)) >>
      messageRepository.addNotification(playerTwoId, RoomACK(roomId, cards(playerTwoId).value)) >>
      gameRepository.updateRoomDeck(roomId, deck)

  }

}
