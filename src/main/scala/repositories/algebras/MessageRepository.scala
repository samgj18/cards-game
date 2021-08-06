package com.evolution
package repositories.algebras

import domain.Message
import domain.Player.PlayerId

import cats.effect.std.Queue

trait MessageRepository[F[_]] {
  def addNotification(playerId: PlayerId, message: Message): F[Unit]
  def removeAllNotifications(playerId: PlayerId): F[Unit]
  def getNotifications(playerId: PlayerId): F[Message]
  def addPlayerToNotificationPool(playerId: PlayerId, players: PlayerId*): F[Queue[F, Message]]
  def removePlayerFromNotificationPool(playerId: PlayerId): F[Unit]
}
