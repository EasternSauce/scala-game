package com.easternsauce.model.events

sealed trait UpdateEvent

case class CreatureDeathEvent(creatureId: String) extends UpdateEvent

case class AbilityChannelStartEvent(creatureId: String, abilityId: String) extends UpdateEvent

case class AbilityActiveStartEvent(creatureId: String, abilityId: String) extends UpdateEvent
