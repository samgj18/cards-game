package com.evolution
package domain

import domain.Action._
import domain.Participant._
import domain.Result._

sealed trait Game

object Game {

  case object SingleCard extends Game
  case object DoubleCard extends Game

  def generate: List[Card] =
    (2 to 14).flatMap(value => List.fill(4)(Card(value))).toList

  def game(
      playerOneAction: Option[Action],
      playerTwoAction: Option[Action],
      playerOne: Player,
      playerTwo: Player
  ): Result = {
    (playerOneAction, playerTwoAction) match {
      case (Some(fAction), Some(sAction)) =>
        (fAction, sAction) match {
          case (Fold, Fold)         => Finished(Loser(playerOne, 1), Loser(playerTwo, 1))
          case (PlayCard, Fold)     => Finished(Winner(playerOne, 3), Loser(playerTwo, 3))
          case (Fold, PlayCard)     => Finished(Loser(playerOne, 3), Winner(playerTwo, 3))
          case (PlayCard, PlayCard) =>
            Draw(playerOne, playerTwo) // Check value of card and return Finished or Draw
        }
      case (None, Some(_))                => InProgress(playerOne)
      case (Some(_), None)                => InProgress(playerTwo)
      case (None, None)                   => NotStarted
    }
  }
}
