package com.easternsauce.event

sealed trait CollisionEvent

case class EntityAbilityCollision(creatureId: String, abilityId: String) extends CollisionEvent
