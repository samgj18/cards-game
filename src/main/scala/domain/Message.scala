package com.evolution
package domain

sealed trait Message

object Message {
  final case class General(value: String) extends Message
  case object Hurry                       extends Message
  case object Lost                        extends Message
  case object Win                         extends Message
  case object Done                        extends Message
}
