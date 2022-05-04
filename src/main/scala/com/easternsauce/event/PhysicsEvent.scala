package com.easternsauce.event

import com.easternsauce.view.physics.terrain.AreaGateBody

sealed trait PhysicsEvent

case class AbilityComponentCollisionEvent(
  creatureId: String,
  abilityId: String,
  componentId: String,
  collidedCreatureId: String
) extends PhysicsEvent

case class AreaGateCollisionStartEvent(creatureId: String, areaGate: AreaGateBody) extends PhysicsEvent

case class AreaGateCollisionEndEvent(creatureId: String) extends PhysicsEvent

case class LootPileCollisionStartEvent(creatureId: String, areaId: String, lootPileId: String) extends PhysicsEvent

case class LootPileCollisionEndEvent(creatureId: String, areaId: String, lootPileId: String) extends PhysicsEvent
