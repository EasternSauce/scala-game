package com.easternsauce.model.actions

import com.easternsauce.model.GameState
import com.easternsauce.model.area.loot.LootPile
import com.easternsauce.model.creature.{Creature, CreatureParams}
import com.easternsauce.model.event._
import com.easternsauce.model.item.{Item, ItemTemplate}
import com.easternsauce.system.Random
import com.softwaremill.quicklens._

trait AreaActions {
  this: GameState =>

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

  def resetArea(areaId: String): GameState = {
    val area = areas(areaId)

    val oldEnemiesIds = area.creatures.filter(creatures(_).isEnemy)

    val newEnemies =
      area.spawnPoints.map(spawnPoint => generateEnemy(spawnPoint.enemyType, areaId, spawnPoint.x, spawnPoint.y))

    val updatePhysicsEvents = newEnemies.map(enemy => UpdatePhysicsOnEnemySpawnEvent(enemy.params.id)) ++ oldEnemiesIds
      .map(enemyId => UpdatePhysicsOnEnemyDespawnEvent(enemyId))

    val updateRendererEvents =
      newEnemies.map(enemy => UpdateRendererOnEnemySpawnEvent(enemy.params.id)) ++ oldEnemiesIds
        .map(enemyId => UpdateRendererOnEnemyDespawnEvent(enemyId))

    this
      .modify(_.creatures)
      .setTo(this.creatures -- oldEnemiesIds ++ newEnemies.map(enemy => (enemy.params.id -> enemy)).toMap)
      .modify(_.areas.at(areaId).creatures)
      .setTo(this.areas(areaId).creatures.filterNot(oldEnemiesIds.toSet) ++ newEnemies.map(_.params.id))
      .modify(_.events)
      .setTo((updatePhysicsEvents ++ updateRendererEvents) ::: this.events)
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

  def spawnLootPile(areaId: String, posX: Float, posY: Float, dropTable: Map[String, Float]): GameState = {

    val items = (for ((key, value) <- dropTable.toList) yield {
      if (Random.nextFloat() < value) {
        Some(Item(ItemTemplate.templates(key)))
      } else None
    }).flatten

    if (items.nonEmpty) {
      val lootPile = LootPile(posX, posY, items)

      val lootPileId = "lootPile" + this.areas(areaId).params.lootPileCounter
      this
        .modify(_.areas.at(areaId).params.lootPiles)
        .setTo(this.areas(areaId).params.lootPiles + (lootPileId -> lootPile))
        .modify(_.areas.at(areaId).params.lootPileCounter)
        .using(_ + 1)
        .modify(_.events)
        .setTo(
          List(
            UpdatePhysicsOnLootPileSpawnEvent(areaId, lootPileId),
            UpdateRendererOnLootPileSpawnEvent(areaId, lootPileId)
          ) ::: this.events
        )
    } else this

  }
}
