package com.evolution
package domain

import domain.Action.{Fold, PlayCard}
import domain.Participant.{Loser, Winner}
import domain.Player.{PlayerId, Players}
import domain.Result.{Draw, Finished, InProgress}
import domain.Room.Actions

sealed trait Game

object Game {

  case object SingleCard extends Game
  case object DoubleCard extends Game

  def generate: List[Card] =
    (2 to 14).flatMap(value => List.fill(4)(Card(value))).toList

  def singleCardGame(
      playersActions: Actions,
      players: Players,
      current: PlayerId
  ): Result = {
    val notInTurnPlayer    = players.filter(_._1 != current).head._2
    val inTurnPlayer       = players(current)
    val inTurnPlayerAction = playersActions(current)
    playersActions.get(notInTurnPlayer.id) match {
      case Some(notInTurnPlayerAction) =>
        (inTurnPlayerAction, notInTurnPlayerAction) match {
          case (PlayCard, PlayCard) => Draw(inTurnPlayer, notInTurnPlayer)
          case (PlayCard, Fold)     => Finished(Winner(inTurnPlayer, 3), Loser(notInTurnPlayer, 3))
          case (Fold, PlayCard)     => Finished(Loser(inTurnPlayer, 3), Winner(notInTurnPlayer, 3))
          case (Fold, Fold)         => Finished(Loser(inTurnPlayer, 1), Loser(notInTurnPlayer, 1))
        }
      case None                        => InProgress(notInTurnPlayer)
    }
  }
}
