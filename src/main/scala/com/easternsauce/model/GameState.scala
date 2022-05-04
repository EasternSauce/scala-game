package com.easternsauce.model

import com.easternsauce.model.actions._
import com.easternsauce.model.area.Area
import com.easternsauce.model.creature.Creature
import com.easternsauce.model.creature.ability.Ability
import com.easternsauce.model.event._

case class GameState(
  currentPlayerId: String,
  creatures: Map[String, Creature] = Map(),
  areas: Map[String, Area],
  currentAreaId: String,
  events: List[UpdateEvent] = List(),
  inventoryState: InventoryState = InventoryState()
) extends AbilityInteractions
    with InventoryActions
    with AreaActions
    with CreatureActions
    with PhysicsEventQueueActions {

  def player: Creature = creatures(currentPlayerId)

  def abilities(creatureId: String, abilityId: String): Ability = creatures(creatureId).params.abilities(abilityId)

}
