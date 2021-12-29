package com.easternsauce.model

import com.easternsauce.event.{AreaChangeEvent, CollisionEvent, EntityAbilityCollision}
import com.easternsauce.model.area.Area
import com.easternsauce.model.creature.Creature
import com.easternsauce.model.creature.ability.Ability
import com.easternsauce.model.event.{CreatureDeathEvent, UpdateEvent}
import com.softwaremill.quicklens._

import scala.collection.mutable.ListBuffer
import scala.util.chaining.scalaUtilChainingOps

case class GameState(
  player: Creature,
  nonPlayers: Map[String, Creature] = Map(),
  areas: Map[String, Area],
  currentAreaId: String,
  events: List[UpdateEvent] = List()
) extends AbilityInteractions {

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

  def processCreatureAreaChanges(changes: ListBuffer[AreaChangeEvent]): GameState = {
    changes.foldLeft(this) {
      case (gameState, AreaChangeEvent(creatureId, oldAreaId, newAreaId)) =>
        gameState
          .assignCreatureToArea(creatureId, Some(oldAreaId), newAreaId)
          .modify(_.currentAreaId)
          .setToIf(creatureId == gameState.player.params.id)(newAreaId) // change game area
    }
  }

  def clearEventsQueue(): GameState = {
    this.modify(_.events).setTo(List())
  }

  def processCollisions(collisionQueue: ListBuffer[CollisionEvent]): GameState = {
    collisionQueue.foldLeft(this) {
      case (gameState, EntityAbilityCollision(creatureId, abilityId)) =>
        val ability = gameState.abilities(creatureId, abilityId)
        gameState.creatureTakeLifeDamage(creatureId, ability.damage)
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
        .using(_.modifyCreatureAbility(abilityId)(operation(_)))
    } else {
      this
        .modify(_.nonPlayers.at(creatureId))
        .using(_.modifyCreatureAbility(abilityId)(operation(_)))
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

  def creatureOnDeath(creatureId: String): GameState = {
    this.pipe(
      gameState =>
        gameState
          .modify(_.events)
          .setTo(CreatureDeathEvent(creatureId) :: gameState.events)
    )
  }
}
