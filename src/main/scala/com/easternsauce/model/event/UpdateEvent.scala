package com.easternsauce.model.event

import com.easternsauce.model.creature.Creature

sealed trait UpdateEvent

case class CreatureDeathEvent(creatureId: String) extends UpdateEvent

case class ComponentCreateBodyEvent(creatureId: String, abilityId: String, componentId: String) extends UpdateEvent

case class ComponentDestroyBodyEvent(creatureId: String, abilityId: String, componentId: String) extends UpdateEvent

case class AreaChangeEvent(creatureId: String, oldAreaId: String, newAreaId: String, posX: Float, posY: Float)
    extends UpdateEvent

case class EnemySpawnEvent(creatureId: String) extends UpdateEvent

case class EnemyDespawnEvent(creature: Creature) extends UpdateEvent
