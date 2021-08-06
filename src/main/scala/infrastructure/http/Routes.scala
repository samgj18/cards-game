package com.evolution
package infrastructure.http

import domain.Message.Balance
import dto.OrchestrateSubmission
import infrastructure.services.{OrchestratorService, PlayerService}
import repositories.algebras.MessageRepository

import io.circe.syntax._
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
          case Text(t, _) =>
            Async[F].delay(decode[OrchestrateSubmission](t)).flatMap {
              case Left(value)  => Async[F].delay(Text(s"Unknown type: $value")) >> Async[F].unit
              case Right(value) =>
                orchestratorService
                  .orchestrate(value.roomId, value.playerId, value.action)
                  .map(_ => Text(s"Unknown type: $value")) >> Async[F].unit
            }
          case f          => Async[F].delay(Text(s"Unknown type: $f")) >> Async[F].unit
        }

        for {
          player <- playerService.createPlayer(channel)
          queue                              <- messageRepository
                     .addPlayerToNotificationPool(player.id)
          _                                  <- messageRepository.addNotification(player.id, Balance(player.id, 1000))
          toClient: Stream[F, WebSocketFrame] =
            Stream
              .fromQueueUnterminated(queue)
              .map(notification => Text(s"${notification.asJson}"))
          response                           <- WebSocketBuilder[F].build(toClient, fromClient)

        } yield response

    }
}
