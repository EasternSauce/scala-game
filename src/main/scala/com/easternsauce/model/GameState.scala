package com.easternsauce.model

import com.easternsauce.event.{AbilityComponentCollision, AreaGateCollision, LeftAreaGateEvent, PhysicsEvent}
import com.easternsauce.model.area.Area
import com.easternsauce.model.creature.ability.{Ability, AbilityComponent}
import com.easternsauce.model.creature.{Creature, CreatureParams}
import com.easternsauce.model.event.{AreaChangeEvent, CreatureDeathEvent, EnemySpawnEvent, UpdateEvent}
import com.easternsauce.system.Random
import com.softwaremill.quicklens._

import scala.util.chaining.scalaUtilChainingOps

case class GameState(
  player: Creature,
  nonPlayers: Map[String, Creature] = Map(),
  areas: Map[String, Area],
  currentAreaId: String,
  events: List[UpdateEvent] = List(),
  inventoryState: InventoryState = InventoryState()
) extends AbilityInteractions
    with InventoryActions {

  def creatures: Map[String, Creature] = nonPlayers + (player.params.id -> player)

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

  def clearQueues(): GameState = {
    this.modify(_.events).setTo(List())
  }

  def processPhysicsQueue(physicsQueue: List[PhysicsEvent]): GameState = {
    physicsQueue.foldLeft(this) {
      case (gameState, AbilityComponentCollision(creatureId, abilityId, componentId, collidedCreatureId)) =>
        val abilityComponent = gameState.abilities(creatureId, abilityId).components(componentId)

        val attackingDisallowed =
          creatures(creatureId).isControlledAutomatically && creatures(collidedCreatureId).isControlledAutomatically

        if (!attackingDisallowed && !creatures(collidedCreatureId).isEffectActive("immunityFrames")) {
          gameState
            .creatureTakeLifeDamage(collidedCreatureId, abilityComponent.damage)
            .creatureActivateEffect(collidedCreatureId, "immunityFrames", 2f)
            .creatureActivateEffect(collidedCreatureId, "stagger", 0.35f)
        } else gameState
      case (gameState, AreaGateCollision(creatureId, areaGate)) =>
        if (!gameState.creatures(creatureId).params.passedGateRecently) {
          val (fromAreaId: String, toAreaId: String, posX: Float, posY: Float) =
            creatures(creatureId).params.areaId match {
              case areaId if areaId == areaGate.areaFrom =>
                (areaGate.areaFrom, areaGate.areaTo, areaGate.toPosX, areaGate.toPosY)
              case areaId if areaId == areaGate.areaTo =>
                (areaGate.areaTo, areaGate.areaFrom, areaGate.fromPosX, areaGate.fromPosY)
              case _ => ("errorArea", -1, -1)
            }
          gameState
            .modify(_.events)
            .setTo(AreaChangeEvent(creatureId, fromAreaId, toAreaId, posX, posY) :: gameState.events)
        } else gameState

      case (gameState, LeftAreaGateEvent(creatureId)) =>
        gameState
        gameState.modifyGameStateCreature(creatureId) { _.modify(_.params.passedGateRecently).setTo(false) }
    }

  }

  def modifyGameStateCreature(creatureId: String)(operation: Creature => Creature): GameState = {
    if (creatureId == player.params.id) {
      this
        .modify(_.player)
        .using(operation(_))
    } else {
      this
        .modify(_.nonPlayers.at(creatureId))
        .using(operation(_))
    }

  }

  def modifyGameStateAbility(creatureId: String, abilityId: String)(operation: Ability => Ability): GameState = {
    if (creatureId == player.params.id) {
      this
        .modify(_.player)
        .using(_.modifyAbility(abilityId)(operation(_)))
    } else {
      this
        .modify(_.nonPlayers.at(creatureId))
        .using(_.modifyAbility(abilityId)(operation(_)))
    }

  }

  def modifyGameStateAbilityComponent(creatureId: String, abilityId: String, componentId: String)(
    operation: AbilityComponent => AbilityComponent
  ): GameState = {
    if (creatureId == player.params.id) {
      this
        .modify(_.player)
        .using(_.modifyAbilityComponent(abilityId, componentId)(operation(_)))
    } else {
      this
        .modify(_.nonPlayers.at(creatureId))
        .using(_.modifyAbilityComponent(abilityId, componentId)(operation(_)))
    }

  }

  def creatureTakeLifeDamage(creatureId: String, damage: Float): GameState = {
    val beforeLife = creatures(creatureId).params.life

    val actualDamage = damage * 100f / (100f + creatures(creatureId).params.totalArmor)

    this
      .modifyGameStateCreature(creatureId)(
        _.pipe(
          creature =>
            if (creature.params.life - actualDamage > 0)
              creature.modify(_.params.life).setTo(creature.params.life - actualDamage)
            else creature.modify(_.params.life).setTo(0f)
        )
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
          .setTo(CreatureDeathEvent(creatureId) :: gameState.events)
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

    this
      .modify(_.nonPlayers)
      .setTo(this.nonPlayers -- oldEnemiesIds ++ newEnemies.map(enemy => (enemy.params.id -> enemy)).toMap)
      .modify(_.areas.at(areaId).creatures)
      .setTo(this.areas(areaId).creatures.filterNot(oldEnemiesIds.toSet) ++ newEnemies.map(_.params.id))
      .modify(_.events)
      .setTo(this.events ++ newEnemies.map(enemy => EnemySpawnEvent(enemy.params.id)))
  }
}
