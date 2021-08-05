package com.evolution
package repositories.algebras

import domain.Player.{PlayerId, Token}
import domain.{Participant, Player}

trait PlayerRepository[F[_]] {
  def updateLiquidity(participant: Participant): F[Boolean]
  def addLiquidity(player: Player, token: Token): F[Unit]
  def getCurrentLiquidity(playerId: PlayerId): F[Token]
}
