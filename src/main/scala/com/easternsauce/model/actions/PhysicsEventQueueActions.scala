package com.easternsauce.model.actions

import com.easternsauce.event._
import com.easternsauce.model.GameState
import com.easternsauce.model.event.{AreaChangeEvent, UpdatePhysicsOnAreaChangeEvent}
import com.easternsauce.model.util.EnhancedChainingSyntax.enhancedScalaUtilChainingOps
import com.softwaremill.quicklens._

trait PhysicsEventQueueActions {
  this: GameState =>

  def clearEventQueue(): GameState = {
    this.modify(_.events).setTo(List())
  }

  def processPhysicsEventQueue(physicsEventQueue: List[PhysicsEvent]): GameState = {
    physicsEventQueue.foldLeft(this) {
      case (gameState, AbilityComponentCollisionEvent(creatureId, abilityId, componentId, collidedCreatureId)) =>
        if (gameState.creatures.contains(creatureId)) {
          val ability = gameState.abilities(creatureId, abilityId)
          val abilityComponent = ability.components(componentId)

          val attackingDisallowed =
            creatures(creatureId).isControlledAutomatically && creatures(collidedCreatureId).isControlledAutomatically

          gameState
            .pipeIf(
              creatures(collidedCreatureId).isAlive && !attackingDisallowed && !creatures(collidedCreatureId)
                .isEffectActive("immunityFrames")
            )(
              _.creatureTakeLifeDamage(
                collidedCreatureId,
                if (ability.isWeaponAttack) creatures(creatureId).weaponDamage else abilityComponent.damage,
                creatures(creatureId).params.posX,
                creatures(creatureId).params.posY
              ).creatureActivateEffect(collidedCreatureId, "immunityFrames", 1f)
                .creatureActivateEffect(collidedCreatureId, "stagger", 0.35f)
                .onAbilityComponentCollision(creatureId, abilityId, componentId)
            )
        } else gameState

      case (gameState, AreaGateCollisionStartEvent(creatureId, areaGate)) =>
        if (
          gameState.creatures.contains(creatureId) && gameState
            .creatures(creatureId)
            .isPlayer && !gameState.creatures(creatureId).params.passedGateRecently
        ) {
          val (fromAreaId: String, toAreaId: String, posX: Float, posY: Float) =
            creatures(creatureId).params.areaId match {
              case areaId if areaId == areaGate.area1Id =>
                (areaGate.area1Id, areaGate.area2Id, areaGate.x2, areaGate.y2)
              case areaId if areaId == areaGate.area2Id =>
                (areaGate.area2Id, areaGate.area1Id, areaGate.x1, areaGate.y1)
              case _ => new RuntimeException("incorrect area for collision")
            }
          gameState
            .modify(_.events)
            .using(
              _.prependedAll(
                List(
                  AreaChangeEvent(creatureId, fromAreaId, toAreaId, posX, posY),
                  UpdatePhysicsOnAreaChangeEvent(creatureId, fromAreaId, toAreaId, posX, posY)
                )
              )
            )
        } else gameState

      case (gameState, AreaGateCollisionEndEvent(creatureId)) =>
        if (gameState.creatures.contains(creatureId) && gameState.creatures(creatureId).isPlayer) {
          gameState
            .modifyGameStateCreature(creatureId) {
              _.modify(_.params.passedGateRecently).setTo(false)
            }
        } else gameState

      case (gameState, LootPileCollisionStartEvent(creatureId, areaId, lootPileId)) =>
        if (gameState.creatures.contains(creatureId)) {
          gameState
            .modify(_.lootPilePickupMenu.visibleLootPiles)
            .setToIf(
              gameState.creatures(creatureId).isPlayer &&
                !gameState.lootPilePickupMenu.visibleLootPiles.contains((areaId, lootPileId))
            )((areaId, lootPileId) :: gameState.lootPilePickupMenu.visibleLootPiles)
        } else gameState
      case (gameState, LootPileCollisionEndEvent(creatureId, areaId, lootPileId)) =>
        if (gameState.creatures.contains(creatureId))
          gameState
            .modify(_.lootPilePickupMenu.visibleLootPiles)
            .setToIf(gameState.creatures(creatureId).isPlayer)(
              gameState.lootPilePickupMenu.visibleLootPiles.filterNot(Set((areaId, lootPileId)))
            )
        else gameState
    }
  }

}
