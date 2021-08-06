package com.evolution
package infrastructure.services

import domain.Game.generate
import domain.Message.RoomACK
import domain.Player.PlayerId
import domain.Room.{Cards, RoomId}
import domain.{Card, Game, Room}
import repositories.algebras.{GameRepository, MessageRepository, PlayerRepository}

import cats.effect.Sync
import cats.implicits._

import java.util.UUID.randomUUID
import scala.util.Random

trait RoomManager[F[_]] {
  def createRoom(game: Game): F[Room]
  def deleteRoom(roomId: RoomId): F[Boolean]
  def dispatcher(
      deck: List[Card],
      playerOneId: PlayerId,
      playerTwoId: PlayerId
  ): F[(Cards, List[Card])]
}

object RoomService         {
  def make[F[_]: Sync](
      gameRepository: GameRepository[F],
      messageRepository: MessageRepository[F],
      playerRepository: PlayerRepository[F]
  ): F[RoomService[F]] = {
    Sync[F].delay(new RoomService[F](gameRepository, messageRepository, playerRepository))
  }
}

class RoomService[F[_]: Sync](
    gameRepository: GameRepository[F],
    messageRepository: MessageRepository[F],
    playerRepository: PlayerRepository[F]
) extends RoomManager[F] {

  def createRoom(game: Game): F[Room]        = {
    gameRepository.getPlayersFromLobby
      .flatMap(players => {
        val (one, two) = (players._1, players._2)
        Sync[F]
          .delay(randomUUID.toString)
          .flatMap(id =>
            dispatcher(generate, one.id, two.id).flatMap(s => {
              val room = Room(
                id,
                game,
                Map(one.id -> one, two.id -> two),
                playersActions = Map.empty,
                s._2,
                s._1
              )
              gameRepository.addGameInProgress(room) >> messageRepository
                .addNotification(one.id, RoomACK(id, s._1(one.id).value)) >>
                messageRepository
                  .addNotification(two.id, RoomACK(id, s._1(two.id).value)) as
                room
            })
          )
      })
  }

  def deleteRoom(roomId: RoomId): F[Boolean] = {
    for {
      maybePlayers <- gameRepository.getPlayersInRoom(roomId)
      _            <- maybePlayers match {
             case Some(players) => gameRepository.addPlayerToLobby(players._1, players._2)
             case None          => Sync[F].unit
           }
      removed      <- gameRepository.removeGameInProgress(roomId)
    } yield removed
  }

  /**
    * The dispatcher acts as the dealer in a real life game. This particular one is fixed to deliver
    * two cards per call.
    */
  def dispatcher(
      deck: List[Card],
      playerOneId: PlayerId,
      playerTwoId: PlayerId
  ): F[(Cards, List[Card])]                  = {
    Sync[F].delay({
      val random         = new Random
      val length         = deck.length
      val indexPlayerOne = random.nextInt(length)
      val indexPlayerTwo = random.nextInt(length - 1)
      val newMap         = Map(playerOneId -> deck(indexPlayerOne), playerTwoId -> deck(indexPlayerTwo))
      val newList        = deck.zipWithIndex
        .filter(list => list._2 != indexPlayerOne && list._2 != indexPlayerTwo)
        .map(_._1)
      (newMap, newList)
    })
  }
}
