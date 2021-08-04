package com.evolution
package infrastructure.services

import domain.Participant.Winner
import domain.Player
import repositories.algebras._

import cats.effect.Sync
import cats.implicits._

import java.util.UUID.randomUUID

trait PlayerManager[F[_]] {
  def createPlayer(name: String): F[Player]
}

object PlayerService         {
  def make[F[_]: Sync](
      playerRepository: PlayerRepository[F],
      gameRepository: GameRepository[F],
      messageRepository: MessageRepository[F]
  ): F[PlayerService[F]] = {
    Sync[F].delay(new PlayerService[F](playerRepository, gameRepository, messageRepository))
  }
}

class PlayerService[F[_]: Sync](
    private val playerRepository: PlayerRepository[F],
    private val gameRepository: GameRepository[F],
    private val messageRepository: MessageRepository[F]
) extends PlayerManager[F] {
  def createPlayer(name: String): F[Player] =
    Sync[F].delay(randomUUID().toString).flatMap { id =>
      val player = Player(name, name)
      playerRepository.addLiquidity(player, 1000) >> gameRepository
        .addPlayerToLobby(player) >> messageRepository
        .addPlayerToNotificationPool(player.id) as player
    }
}
