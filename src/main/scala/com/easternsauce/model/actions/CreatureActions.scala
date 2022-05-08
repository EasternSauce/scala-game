package com.easternsauce.model.actions

import com.easternsauce.model.GameState
import com.easternsauce.model.creature.Creature
import com.easternsauce.model.creature.ability.{Ability, AbilityComponent}
import com.easternsauce.model.event.UpdatePhysicsOnCreatureDeathEvent
import com.easternsauce.util.Vector2Wrapper
import com.softwaremill.quicklens._

import scala.util.chaining.scalaUtilChainingOps

trait CreatureActions {
  this: GameState =>

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
          .setTo(15f)
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
    val creature = creatures(creatureId)

    val abilityComponentCombinations: List[(String, String)] =
      this.creatures(creatureId).params.abilities.toList.foldLeft(List[(String, String)]()) {
        case (acc, (k, v)) => acc ++ List().zipAll(v.components.keys.toList, k, "")
      }

    this.pipe(
      gameState =>
        gameState
          .modify(_.events)
          .setTo(UpdatePhysicsOnCreatureDeathEvent(creatureId) :: gameState.events)
          .modifyGameStateCreature(creatureId)(_.onDeath().modify(_.params.abilities.each).using(_.stop()))
          .pipe(gameState => {
            abilityComponentCombinations.foldLeft(gameState) {
              case (gameState, (abilityId, componentId)) =>
                gameState.onAbilityComponentInactiveStart(creatureId, abilityId, componentId)
            }
          })
          .spawnLootPile(creature.params.areaId, creature.params.posX, creature.params.posY, creature.dropTable)
    )
  }
}
