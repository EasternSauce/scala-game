package model

import model.creature.Creature

case class GameState(player: Creature, nonPlayers: Map[String, Creature] = Map()) {

  def creatures: Map[String, Creature] = nonPlayers + (player.params.id -> player)
}
