package com.easternsauce.event

sealed trait CollisionEvent

case class AbilityComponentCollision(
  creatureId: String,
  abilityId: String,
  componentId: String,
  collidedCreatureId: String
) extends CollisionEvent
