package com.evolution
package domain

import domain.Player.{PlayerId, Token}
import domain.Room.RoomId

import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import io.circe.syntax._
import io.circe.{Encoder, JsonObject}

sealed trait Message

object Message {
  final case class Balance(playerId: PlayerId, balance: Token) extends Message
  final case class RoomInformation(roomId: RoomId, card: Int)  extends Message
  final case class BackPush(message: String)                   extends Message
  case object Hurry                                            extends Message
  case object Lost                                             extends Message
  case object Win                                              extends Message
  case object Done                                             extends Message

  implicit val encoder: Encoder[Message] = Encoder[JsonObject].contramap {
    case Hurry                         => BackPush("hurry").asJsonObject
    case Lost                          => BackPush("lost").asJsonObject
    case Win                           => BackPush("win").asJsonObject
    case Done                          => BackPush("done").asJsonObject
    case Balance(playerId, token)      => Balance(playerId, token).asJsonObject
    case RoomInformation(roomId, card) => RoomInformation(roomId, card).asJsonObject
    case BackPush(message)             => BackPush(message).asJsonObject
  }
}
