package layers.model_layer.gamestate

import layers.model_layer.gamestate.creature.{Creature, Player}

case class GameState(player: Player, nonPlayers: Map[String, Creature] = Map()) {

  def creatures: Map[String, Creature] =  nonPlayers + (player.params.id -> player)
}