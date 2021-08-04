package com.evolution
package repositories.interpreters

import domain.Participant.{Loser, Winner}
import domain.Player.{Liquidity, Token}
import domain.{Participant, Player}
import repositories.algebras.PlayerRepository

import cats.effect._
import cats.implicits._

object InMemoryPlayerRepository {
  def make[F[_]: Sync]: F[PlayerRepository[F]] = {
    for {
      liquidity <- Ref.of[F, Liquidity](Map.empty)
    } yield new InMemoryPlayerRepository[F](liquidity)
  }
}

class InMemoryPlayerRepository[F[_]: Sync](
    private val liquidity: Ref[F, Liquidity]
) extends PlayerRepository[F] {

  def updateLiquidity(participant: Participant): F[Boolean] = {
    participant match {
      case Winner(player, revenue) =>
        liquidity.modify(current =>
          current.get(player.id) match {
            case Some(value) => (current + (player.id -> (value + revenue)), true)
            case None        => (current, false)
          }
        )
      case Loser(player, lost)     =>
        liquidity.modify(current =>
          current.get(player.id) match {
            case Some(value) => (current + (player.id -> (value - lost)), true)
            case None        => (current, false)
          }
        )
    }
  }

  def addLiquidity(player: Player, token: Token): F[Unit]   = {
    liquidity.update(current => {
      current + (player.id -> token)
    })
  }
}
