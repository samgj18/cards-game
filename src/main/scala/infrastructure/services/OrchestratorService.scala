package com.evolution
package infrastructure.services

import domain.Action
import domain.Player.PlayerId
import domain.Room.RoomId

import cats.effect._
import cats.implicits._

trait OrchestratorManager[F[_]] {
  def orchestrate(roomId: RoomId, playerId: PlayerId, action: Action): F[Boolean]
}

object OrchestratorService         {
  def make[F[_]: Async](
      roomService: RoomService[F],
      actionService: ActionService[F]
  ): F[OrchestratorService[F]] = {
    Async[F].delay(new OrchestratorService[F](roomService, actionService))
  }
}

class OrchestratorService[F[_]: Async](
    roomService: RoomService[F],
    actionService: ActionService[F]
) extends OrchestratorManager[F] {

  /**
    * The orchestrator keeps a vigilant eye upon every user request to guide him through upto
    * completion of the game.
    */
  def orchestrate(roomId: RoomId, playerId: PlayerId, action: Action): F[Boolean] = {
    for {
      result     <- actionService.performer(roomId, playerId, action)
      keepAlive  <- actionService.decider(result)
      completion <- if (keepAlive.value) Sync[F].delay(false)
                    else roomService.deleteRoom(roomId)
    } yield completion
  }
}
