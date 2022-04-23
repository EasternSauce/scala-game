package com.easternsauce.event

import com.easternsauce.view.physics.terrain.AreaGate

sealed trait PhysicsEvent

case class AbilityComponentCollision(
  creatureId: String,
  abilityId: String,
  componentId: String,
  collidedCreatureId: String
) extends PhysicsEvent

case class AreaGateCollision(creatureId: String, areaGate: AreaGate) extends PhysicsEvent

case class LeftAreaGateEvent(creatureId: String) extends PhysicsEvent
