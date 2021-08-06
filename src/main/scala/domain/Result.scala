package com.evolution
package domain

import domain.Room.RoomId

final case class Card(value: Int) extends AnyVal

sealed trait Result

object Result {
  final case class Finished(one: Participant, two: Participant) extends Result
  final case class Draw(room: Room, one: Player, two: Player)   extends Result
  final case class InProgress(turn: Player)                     extends Result
  case object NotStarted                                        extends Result
}
