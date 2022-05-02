package com.easternsauce.model

import com.easternsauce.event.{AbilityComponentCollisionEvent, AreaGateCollisionEvent, LeftAreaGateEvent, PhysicsEvent}
import com.easternsauce.model.area.Area
import com.easternsauce.model.creature.ability.{Ability, AbilityComponent}
import com.easternsauce.model.creature.{Creature, CreatureParams}
import com.easternsauce.model.event._
import com.easternsauce.system.Random
import com.easternsauce.util.Vector2Wrapper
import com.softwaremill.quicklens._

import scala.util.chaining.scalaUtilChainingOps

case class GameState(
  currentPlayerId: String,
  creatures: Map[String, Creature] = Map(),
  areas: Map[String, Area],
  currentAreaId: String,
  events: List[UpdateEvent] = List(),
  inventoryState: InventoryState = InventoryState()
) extends AbilityInteractions
    with InventoryActions {

  def player: Creature = creatures(currentPlayerId)

  def abilities(creatureId: String, abilityId: String): Ability = creatures(creatureId).params.abilities(abilityId)

  def assignCreatureToArea(creatureId: String, oldAreaId: Option[String], newAreaId: String): GameState = {
    if (oldAreaId.nonEmpty) {
      modifyGameStateCreature(creatureId) {
        _.modify(_.params.areaId).setTo(newAreaId)
      } // set creature area id to new area id
        .modify(_.areas.at(oldAreaId.get).creatures)
        .using(_.filter(_ != creatureId)) // remove creature id from old area
        .modify(_.areas.at(newAreaId).creatures)
        .using(creatureId :: _) // add creature id to new area
    } else {
      modifyGameStateCreature(creatureId) {
        _.modify(_.params.areaId).setTo(newAreaId)
      } // set creature area id to new area id
        .modify(_.areas.at(newAreaId).creatures)
        .using(creatureId :: _) // add creature id to new area
    }

  }

  def processCreatureAreaChanges(): GameState = {
    events.foldLeft(this) {
      case (gameState, AreaChangeEvent(creatureId, oldAreaId, newAreaId, posX, posY)) =>
        gameState
          .resetArea(newAreaId)
          .assignCreatureToArea(creatureId, Some(oldAreaId), newAreaId)
          .modifyGameStateCreature(creatureId) {
            _.setPosition(posX, posY)
              .modify(_.params.passedGateRecently)
              .setTo(true)
          }
          .modify(_.currentAreaId)
          .setToIf(creatureId == gameState.player.params.id)(newAreaId) // change game area

      case (gameState, _) => gameState
    }
  }

  def clearEventQueue(): GameState = {
    this.modify(_.events).setTo(List())
  }

  def processPhysicsEventQueue(physicsEventQueue: List[PhysicsEvent]): GameState = {
    physicsEventQueue.foldLeft(this) {
      case (gameState, AbilityComponentCollisionEvent(creatureId, abilityId, componentId, collidedCreatureId)) =>
        val abilityComponent = gameState.abilities(creatureId, abilityId).components(componentId)

        val attackingDisallowed =
          creatures(creatureId).isControlledAutomatically && creatures(collidedCreatureId).isControlledAutomatically

        gameState
          .pipe(
            gameState =>
              if (!attackingDisallowed && !creatures(collidedCreatureId).isEffectActive("immunityFrames")) {
                gameState
                  .creatureTakeLifeDamage(
                    collidedCreatureId,
                    abilityComponent.damage,
                    creatures(creatureId).params.posX,
                    creatures(creatureId).params.posY
                  )
                  .creatureActivateEffect(collidedCreatureId, "immunityFrames", 2f)
                  .creatureActivateEffect(collidedCreatureId, "stagger", 0.35f)
              } else gameState
          )

      case (gameState, AreaGateCollisionEvent(creatureId, areaGate)) =>
        gameState
          .pipe(
            gameState =>
              if (!gameState.creatures(creatureId).params.passedGateRecently) {
                val (fromAreaId: String, toAreaId: String, posX: Float, posY: Float) =
                  creatures(creatureId).params.areaId match {
                    case areaId if areaId == areaGate.areaFrom =>
                      (areaGate.areaFrom, areaGate.areaTo, areaGate.toPosX, areaGate.toPosY)
                    case areaId if areaId == areaGate.areaTo =>
                      (areaGate.areaTo, areaGate.areaFrom, areaGate.fromPosX, areaGate.fromPosY)
                    case _ => new RuntimeException("incorrect area for collision")
                  }
                gameState
                  .modify(_.events)
                  .setTo(
                    List(
                      AreaChangeEvent(creatureId, fromAreaId, toAreaId, posX, posY),
                      UpdatePhysicsOnAreaChangeEvent(creatureId, fromAreaId, toAreaId, posX, posY)
                    ) ::: gameState.events
                  )
              } else gameState
          )

      case (gameState, LeftAreaGateEvent(creatureId)) =>
        gameState
          .modifyGameStateCreature(creatureId) {
            _.modify(_.params.passedGateRecently).setTo(false)
          }
    }
  }

  def modifyGameStateCreature(creatureId: String)(operation: Creature => Creature): GameState = {

    this
      .modify(_.creatures.at(creatureId))
      .using(operation(_))

  }

  def modifyGameStateAbility(creatureId: String, abilityId: String)(operation: Ability => Ability): GameState = {

    this
      .modify(_.creatures.at(creatureId))
      .using(_.modifyAbility(abilityId)(operation(_)))

  }

  def modifyGameStateAbilityComponent(creatureId: String, abilityId: String, componentId: String)(
    operation: AbilityComponent => AbilityComponent
  ): GameState = {

    this
      .modify(_.creatures.at(creatureId))
      .using(_.modifyAbilityComponent(abilityId, componentId)(operation(_)))

  }

  def creatureTakeLifeDamage(creatureId: String, damage: Float, sourcePosX: Float, sourcePosY: Float): GameState = {
    val beforeLife = creatures(creatureId).params.life

    val actualDamage = damage * 100f / (100f + creatures(creatureId).params.totalArmor)

    this
      .modifyGameStateCreature(creatureId)(
        _.pipe(
          creature =>
            if (creature.params.life - actualDamage > 0)
              creature.modify(_.params.life).setTo(creature.params.life - actualDamage)
            else creature.modify(_.params.life).setTo(0f)
        ).activateEffect("knockback", 0.15f)
          .modify(_.params.knockbackDir)
          .setTo(
            Vector2Wrapper(
              creatures(creatureId).params.posX - sourcePosX,
              creatures(creatureId).params.posY - sourcePosY
            ).normal
          )
          .modify(_.params.knockbackVelocity)
          .setTo(10f)
      )
      .pipe(gameState => {
        val creature = gameState.creatures(creatureId)
        if (beforeLife > 0f && creature.params.life <= 0f) {
          gameState.creatureOnDeath(creatureId)
        } else gameState
      })
  }

  def creatureActivateEffect(creatureId: String, effectName: String, effectTime: Float): GameState = {
    this
      .modifyGameStateCreature(creatureId)(_.activateEffect(effectName, effectTime))
  }

  def creatureOnDeath(creatureId: String): GameState = {
    this.pipe(
      gameState =>
        gameState
          .modify(_.events)
          .setTo(UpdatePhysicsOnCreatureDeathEvent(creatureId) :: gameState.events)
          .modifyGameStateCreature(creatureId)(_.onDeath())
    )
  }

  def generateEnemy(enemyType: String, areaId: String, posX: Float, posY: Float): Creature = {
    val creatureId = enemyType + "_" + Math.abs(Random.nextInt())

    val action = Class
      .forName("com.easternsauce.model.creature." + enemyType)
      .getMethod("apply", classOf[CreatureParams])
      .invoke(
        null,
        CreatureParams(
          id = creatureId,
          posX = posX,
          posY = posY,
          areaId = areaId,
          life = 100f,
          maxLife = 100f,
          stamina = 100f,
          maxStamina = 100f
        )
      )

    action.asInstanceOf[Creature].init()
  }

  def resetArea(areaId: String): GameState = {
    val area = areas(areaId)

    val oldEnemiesIds = area.creatures.filter(creatures(_).isEnemy)

    val newEnemies =
      area.spawnPoints.map(spawnPoint => generateEnemy(spawnPoint.enemyType, areaId, spawnPoint.x, spawnPoint.y))

    val updatePhysicsEvents = newEnemies.map(enemy => UpdatePhysicsOnEnemySpawnEvent(enemy.params.id)) ++ oldEnemiesIds
      .map(enemyId => UpdatePhysicsOnEnemyDespawnEvent(creatures(enemyId)))

    val updateRendererEvents =
      newEnemies.map(enemy => UpdateRendererOnEnemySpawnEvent(enemy.params.id)) ++ oldEnemiesIds
        .map(enemyId => UpdateRendererOnEnemyDespawnEvent(creatures(enemyId)))

    this
      .modify(_.creatures)
      .setTo(this.creatures -- oldEnemiesIds ++ newEnemies.map(enemy => (enemy.params.id -> enemy)).toMap)
      .modify(_.areas.at(areaId).creatures)
      .setTo(this.areas(areaId).creatures.filterNot(oldEnemiesIds.toSet) ++ newEnemies.map(_.params.id))
      .modify(_.events)
      .setTo(this.events ++ updatePhysicsEvents ++ updateRendererEvents)
  }
}
