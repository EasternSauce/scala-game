package com.easternsauce.model

import com.easternsauce.model.area.Area
import com.easternsauce.model.creature.Creature
import com.easternsauce.model.creature.ability.Ability
import com.softwaremill.quicklens._

case class GameState(
  player: Creature,
  nonPlayers: Map[String, Creature] = Map(),
  areas: Map[String, Area],
  currentAreaId: String
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

  def processCreatureAreaChanges(changes: List[(String, String, String)]): GameState = {
    changes.foldLeft(this) {
      case (gameState, (creatureId, oldAreaId, newAreaId)) =>
        gameState
          .assignCreatureToArea(creatureId, Some(oldAreaId), newAreaId)
          .modify(_.currentAreaId)
          .setToIf(creatureId == gameState.player.params.id)(newAreaId) // change game area
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
}
