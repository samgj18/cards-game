package com.evolution
package infrastructure.http

import dto.OrchestrateSubmission
import infrastructure.services.{OrchestratorService, PlayerService, RoomService}
import repositories.algebras.MessageRepository

import cats.effect._
import cats.implicits._
import io.circe.jawn._
import fs2._
import org.http4s._
import org.http4s.dsl._
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.{Close, Text}

object Routes            {
  def make[F[_]: Async](
      playerService: PlayerService[F],
      orchestratorService: OrchestratorService[F],
      messageRepository: MessageRepository[F]
  ): HttpRoutes[F] = {
    new Routes[F](playerService, orchestratorService, messageRepository).routes
  }
}

class Routes[F[_]: Async](
    playerService: PlayerService[F],
    orchestratorService: OrchestratorService[F],
    messageRepository: MessageRepository[F]
) extends Http4sDsl[F] {
  val routes: HttpRoutes[F] = game

  private def game: HttpRoutes[F] =
    HttpRoutes.of[F] {

      case GET -> Root / "ws" / channel =>
        val fromClient: Pipe[F, WebSocketFrame, Unit] = _.evalMap {
          case Text(t, _)   =>
            Async[F].delay(decode[OrchestrateSubmission](t)).flatMap {
              case Left(value)  => Async[F].delay(Text(s"Unknown type: $value")) >> Async[F].unit
              case Right(value) =>
                orchestratorService
                  .orchestrate(value.roomId, value.playerId, value.action)
                  .map(_ => Text(s"Unknown type: $value")) >> Async[F].unit
            }
          case close: Close =>
            ??? // Remove players, do clean up, make the other one winner and notify
          case f => Async[F].delay(Text(s"Unknown type: $f")) >> Async[F].unit
        }

        playerService.createPlayer(channel) >>
          messageRepository
            .addPlayerToNotificationPool(channel)
            .flatMap { queue =>
              val toClient: Stream[F, WebSocketFrame] =
                Stream.fromQueueUnterminated(queue).map(notification => Text(s"$notification"))
              WebSocketBuilder[F].build(toClient, fromClient)
            }
    }
}
