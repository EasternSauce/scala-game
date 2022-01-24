package com.easternsauce.model.event

sealed trait UpdateEvent

case class CreatureDeathEvent(creatureId: String) extends UpdateEvent

case class ComponentCreateBodyEvent(creatureId: String, abilityId: String, componentId: String) extends UpdateEvent

case class ComponentDestroyBodyEvent(creatureId: String, abilityId: String, componentId: String) extends UpdateEvent
