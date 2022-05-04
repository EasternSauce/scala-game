package com.easternsauce.model

import com.easternsauce.model.actions._
import com.easternsauce.model.area.Area
import com.easternsauce.model.creature.Creature
import com.easternsauce.model.creature.ability.Ability
import com.easternsauce.model.event._
import com.easternsauce.model.hud.{InventoryWindow, LootPilePickupMenu}

case class GameState(
  currentPlayerId: String,
  creatures: Map[String, Creature] = Map(),
  areas: Map[String, Area],
  currentAreaId: String,
  events: List[UpdateEvent] = List(),
  inventoryWindow: InventoryWindow = InventoryWindow(),
  lootPilePickupMenu: LootPilePickupMenu = LootPilePickupMenu()
) extends AbilityInteractions
    with InventoryActions
    with AreaActions
    with CreatureActions
    with PhysicsEventQueueActions {

  def player: Creature = creatures(currentPlayerId)

  def abilities(creatureId: String, abilityId: String): Ability = creatures(creatureId).params.abilities(abilityId)

  def lootPilePickupMenuOpen: Boolean = lootPilePickupMenu.visibleLootPiles.nonEmpty && !inventoryWindow.inventoryOpen
}
