package com.evolution
package repositories.interpreters

import domain.Message
import domain.Player.PlayerId
import repositories.algebras.MessageRepository
import repositories.interpreters.InMemoryMessageRepository.Notifications

import cats.effect._
import cats.implicits._
import cats.effect.std.Queue

object InMemoryMessageRepository {
  type Notifications[F[_]] = Map[PlayerId, Queue[F, Message]]

  def make[F[_]: Concurrent]: F[MessageRepository[F]] = {
    for {
      notifications <- Ref.of[F, Notifications[F]](Map.empty)
    } yield new InMemoryMessageRepository[F](notifications)
  }
}

class InMemoryMessageRepository[F[_]: Concurrent](
    private val notifications: Ref[F, Notifications[F]]
) extends MessageRepository[F] {
  def addMessageToNotifications(playerId: PlayerId, message: Message): F[Unit] = {
    notifications.get.flatMap(playerMap => playerMap(playerId).offer(message))
  }

  def addPlayerToNotificationPool(playerId: PlayerId, players: PlayerId*): F[Queue[F, Message]] = {
    Queue.unbounded[F, Message].flatMap { queue =>
      players.traverse(id => notifications.update(playersMap => playersMap + (id -> queue))) >>
        notifications.update(playerMap => playerMap + (playerId -> queue)) as queue
    }
  }

  def getNotifications(playerId: PlayerId): F[Message]                                          = {
    notifications.get.flatMap(playerMap => playerMap(playerId).take)
  }

  def removePlayerFromNotificationPool(playerId: PlayerId): F[Unit] = {
    notifications.update(playerMap => playerMap - playerId)
  }

  def removeAllNotifications(playerId: PlayerId): F[Unit] = {
    notifications.update(notes => notes - playerId)
  }
}
