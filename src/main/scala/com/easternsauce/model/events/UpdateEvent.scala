package com.easternsauce.model.events

sealed trait UpdateEvent

case class CreatureDeathEvent(creatureId: String) extends UpdateEvent

case class AbilityCreateBodyEvent(creatureId: String, abilityId: String) extends UpdateEvent

case class AbilityDestroyBodyEvent(creatureId: String, abilityId: String) extends UpdateEvent
