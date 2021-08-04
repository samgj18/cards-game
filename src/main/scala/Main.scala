package com.evolution

import domain.Game.SingleCard
import infrastructure.http.Routes
import infrastructure.services._
import repositories.interpreters._

import cats.effect._
import cats.implicits._
import fs2._
import org.http4s.HttpApp
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits._

import scala.concurrent.ExecutionContext.global

object Main extends IOApp {

  def dependencies[F[_]: Async]: F[(HttpApp[F], RoomService[F])] =
    for {
      playerRepository    <- InMemoryPlayerRepository.make
      messageRepository   <- InMemoryMessageRepository.make
      gameRepository      <- InMemoryGameRepository.make
      playerService       <- PlayerService.make(playerRepository, gameRepository, messageRepository)
      roomService         <- RoomService.make(gameRepository, messageRepository)
      actionService       <- ActionService.make(gameRepository, playerRepository, messageRepository)
      orchestratorService <- OrchestratorService.make(roomService, actionService)
      app                  = Routes.make(playerService, orchestratorService, messageRepository)
    } yield (app.orNotFound, roomService)

  override def run(args: List[String]): IO[ExitCode]             = {
    Stream
      .eval(dependencies[IO])
      .flatMap {
        case (app, roomService) =>
          val webSocketFrame = BlazeServerBuilder[IO](global)
            .withHttpApp(app)
            .bindHttp(8080, "0.0.0.0") // To bind it to all available network cards
            .serve
          val matchMaker = Stream.repeatEval(roomService.createRoom(SingleCard))
          webSocketFrame.concurrently(matchMaker)
      }
      .compile
      .lastOrError
  }
}
