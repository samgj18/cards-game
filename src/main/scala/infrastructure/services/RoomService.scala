package com.evolution
package infrastructure.services

import domain.Game.generate
import domain.Player.PlayerId
import domain.Room.RoomId
import domain.{Card, Game, Room}
import repositories.algebras.{GameRepository, MessageRepository}

import cats.effect.kernel.Concurrent
import cats.effect.{Async, Sync}
import cats.implicits._
import com.evolution.domain.Message.General

import java.util.UUID.randomUUID
import scala.util.Random

trait RoomManager[F[_]] {
  def createRoom(game: Game): F[Room]
  def deleteRoom(roomId: RoomId): F[Boolean]
}

object RoomService         {
  def make[F[_]: Sync](
      gameRepository: GameRepository[F],
      messageRepository: MessageRepository[F]
  ): F[RoomService[F]] = {
    Sync[F].delay(new RoomService[F](gameRepository, messageRepository))
  }
}

class RoomService[F[_]: Sync](
    gameRepository: GameRepository[F],
    messageRepository: MessageRepository[F]
) extends RoomManager[F] {

  def createRoom(game: Game): F[Room]        = {
    gameRepository.getPlayersFromLobby
      .flatMap(players => {
        val (one, two) = (players._1, players._2)
        Sync[F]
          .delay(randomUUID.toString)
          .flatMap(id =>
            dispatcher(generate, one.id, two.id).flatMap(s => {
              val room = Room(id, game, one, two, None, None, s._2, s._1)
              gameRepository.addGameInProgress(room) >> messageRepository
                .addMessageToNotifications(one.id, General(id)) >> messageRepository
                .addMessageToNotifications(two.id, General(id)) as room
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

  private def dispatcher(
      deck: List[Card],
      playerOneId: PlayerId,
      playerTwoId: PlayerId
  ): F[(Map[PlayerId, Card], List[Card])]    = {
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
