package com.easternsauce.box2d_physics

sealed trait CollisionEvent

case class EntityAbilityCollision(creatureId: String, abilityId: String) extends CollisionEvent