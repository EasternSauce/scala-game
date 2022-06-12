package com.easternsauce.model.event

sealed trait UpdateEvent

case class UpdatePhysicsOnCreatureDeathEvent(creatureId: String) extends UpdateEvent

case class UpdatePhysicsOnComponentCreateBodyEvent(creatureId: String, abilityId: String, componentId: String)
    extends UpdateEvent

case class UpdatePhysicsOnComponentDestroyBodyEvent(creatureId: String, abilityId: String, componentId: String)
    extends UpdateEvent

case class AreaChangeEvent(creatureId: String, oldAreaId: String, newAreaId: String, posX: Float, posY: Float)
    extends UpdateEvent

case class UpdatePhysicsOnAreaChangeEvent(
  creatureId: String,
  oldAreaId: String,
  newAreaId: String,
  posX: Float,
  posY: Float
) extends UpdateEvent

case class UpdatePhysicsOnEnemySpawnEvent(creatureId: String, areaId: String) extends UpdateEvent

case class UpdatePhysicsOnEnemyDespawnEvent(creatureId: String, areaId: String) extends UpdateEvent

case class UpdateRendererOnEnemySpawnEvent(creatureId: String) extends UpdateEvent

case class UpdateRendererOnEnemyDespawnEvent(creatureId: String) extends UpdateEvent

case class UpdatePhysicsOnLootPileSpawnEvent(areaId: String, lootPileId: String) extends UpdateEvent

case class UpdatePhysicsOnLootPileDespawnEvent(areaId: String, lootPileId: String) extends UpdateEvent

case class UpdateRendererOnLootPileSpawnEvent(areaId: String, lootPileId: String) extends UpdateEvent

case class UpdateRendererOnLootPileDespawnEvent(areaId: String, lootPileId: String) extends UpdateEvent

case class PlaySoundEvent(soundId: String) extends UpdateEvent

case class PathfindingRequestEvent(creatureFromId: String) extends UpdateEvent
