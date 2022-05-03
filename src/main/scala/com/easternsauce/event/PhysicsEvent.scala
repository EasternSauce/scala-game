package com.easternsauce.event

import com.easternsauce.view.physics.terrain.AreaGateBody

sealed trait PhysicsEvent

case class AbilityComponentCollisionEvent(
  creatureId: String,
  abilityId: String,
  componentId: String,
  collidedCreatureId: String
) extends PhysicsEvent

case class AreaGateCollisionEvent(creatureId: String, areaGate: AreaGateBody) extends PhysicsEvent

case class LeftAreaGateEvent(creatureId: String) extends PhysicsEvent
