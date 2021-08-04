package com.evolution
package domain

sealed trait Participant

object Participant {
  final case class Winner(player: Player, revenue: Int) extends Participant
  final case class Loser(player: Player, lost: Int)     extends Participant
}
