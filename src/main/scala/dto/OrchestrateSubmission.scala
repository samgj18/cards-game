package com.evolution
package dto

import domain.Action
import domain.Player.PlayerId
import domain.Room.RoomId

import io.circe.{Decoder, Encoder}

object OrchestrateSubmission {
  implicit val decoder: Decoder[OrchestrateSubmission] = {
    Decoder.forProduct3("roomId", "playerId", "action")((roomId, playerId, action) =>
      OrchestrateSubmission(roomId, playerId, action)
    )
  }

  implicit val encoder: Encoder[OrchestrateSubmission] = {
    Encoder.forProduct3("roomId", "playerId", "action")((submission) =>
      (submission.roomId, submission.playerId, submission.action)
    )
  }
}

case class OrchestrateSubmission(roomId: RoomId, playerId: PlayerId, action: Action)
