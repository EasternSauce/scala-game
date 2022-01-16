package com.easternsauce.model

import com.easternsauce.model.creature.ability.AbilityState
import com.easternsauce.model.event.{AbilityCreateBodyEvent, AbilityDestroyBodyEvent}
import com.easternsauce.util.Vector2Wrapper
import com.softwaremill.quicklens._

import scala.util.chaining._

trait AbilityInteractions {
  this: GameState =>

  def onAbilityComponentActiveStart(creatureId: String, abilityId: String, componentId: String): GameState = {

    this
      .pipe(
        gameState => gameState.modify(_.events).setTo(AbilityCreateBodyEvent(creatureId, abilityId) :: gameState.events)
      )
      .modifyGameStateCreature(creatureId) { creature =>
        creature
          .modifyAbilityComponent(abilityId, componentId)(
            _.modify(_.params.abilityActiveAnimationTimer)
              .using(_.restart())
              .modify(_.params.activeTimer)
              .using(_.restart())
          )
          .modifyAbilityComponent(abilityId, componentId)(
            _.modify(_.params.channelTimer)
              .using(_.stop())
          )
          .modifyAbilityComponent(abilityId, componentId)(
            _.modify(_.params.abilityChannelAnimationTimer)
              .using(_.stop())
          )
          .modifyAbilityComponent(abilityId, componentId)(
            _.setDirVector(Vector2Wrapper(creature.params.dirVector.x, creature.params.dirVector.y))
          )
          .modifyAbility(abilityId)(_.updateHitbox(creature))
      }
  }

  def onAbilityComponentChannelStart(creatureId: String, abilityId: String, componentId: String): GameState = {

    this
      .modifyGameStateCreature(creatureId) { creature =>
        creature
          .modifyAbility(abilityId)(_.modify(_.components.at(componentId).params.channelTimer).using(_.restart()))
          .modifyAbility(abilityId)(
            _.modify(_.components.at(componentId).params.abilityChannelAnimationTimer).using(_.restart())
          )
          .modifyAbilityComponent(abilityId, componentId)(
            _.setDirVector(Vector2Wrapper(creature.params.dirVector.x, creature.params.dirVector.y))
          )
          .modifyAbility(abilityId)(_.updateHitbox(creature))
      }

  }

  def onAbilityComponentInactiveStart(creatureId: String, abilityId: String, componentId: String): GameState = {

    this
      .pipe(
        gameState =>
          gameState.modify(_.events).setTo(AbilityDestroyBodyEvent(creatureId, abilityId) :: gameState.events)
      )
      .modifyGameStateCreature(creatureId) { creature =>
        creature
          .modifyAbility(abilityId)(_.modify(_.components.at(componentId).params.activeTimer).using(_.stop()))
          .modifyAbility(abilityId)(
            _.modify(_.components.at(componentId).params.abilityActiveAnimationTimer).using(_.stop())
          )
      }

  }

  def onAbilityComponentChannelUpdate(creatureId: String, abilityId: String): GameState =
    this.modifyGameStateCreature(creatureId)(creature => creature.modifyAbility(abilityId)(_.updateHitbox(creature)))

  def onAbilityComponentActiveUpdate(creatureId: String, abilityId: String): GameState = {
    this.modifyGameStateCreature(creatureId)(creature => creature.modifyAbility(abilityId)(_.updateHitbox(creature)))
  }

  def updateCreatureAbility(creatureId: String, abilityId: String, delta: Float): GameState = {
    val ability = abilities(creatureId, abilityId)

    ability.components.keys.foldLeft(this)((gameState, componentId) => {
      val channelTimer = ability.components(componentId).params.channelTimer
      val activeTimer = ability.components(componentId).params.activeTimer

      import com.easternsauce.model.creature.ability.AbilityState._
      (ability.components(componentId).params.state match {
        case Channel =>
          gameState
            .pipe {
              case state if channelTimer.time > ability.components(componentId).specification.totalChannelTime =>
                state
                  .modifyGameStateAbility(creatureId, abilityId)(
                    _.modify(_.components.at(componentId)).using(_.stop().makeActive())
                  )
                  .onAbilityComponentActiveStart(creatureId, abilityId, componentId)
              case state => state
            }
            .onAbilityComponentChannelUpdate(creatureId, abilityId)
        case Active =>
          gameState
            .pipe {
              case state if activeTimer.time > ability.components(componentId).specification.totalActiveTime =>
                state
                  .modifyGameStateAbility(creatureId, abilityId)(
                    _.modify(_.components.at(componentId)).using(_.stop().makeInactive())
                  )
                  .onAbilityComponentInactiveStart(creatureId, abilityId, componentId)

              case state => state
            }
            .onAbilityComponentActiveUpdate(creatureId, abilityId)
        case Inactive =>
          gameState.pipe(
            state =>
              if (
                ability.params.onCooldown && activeTimer.time > ability.cooldownTime // TODO this won't work; cooldown timer???
              )
                modifyGameStateAbility(creatureId, abilityId)(_.setNotOnCooldown())
              else state
          )
        case _ => gameState
      }).modifyGameStateAbility(creatureId, abilityId)(_.updateComponentTimers(componentId, delta))
    })

  }

  def performAbility(creatureId: String, abilityId: String): GameState = {
    val creature = creatures(creatureId)

    val ability = creature.params.abilities(abilityId)

    if (
      creature.params.stamina > 0 && !ability.componentsActive && !ability.params.onCooldown
      /*&& !creature.abilityActive*/
    ) {
      ability.components.keys
        .foldLeft(this)((gameState, componentId) => {
          gameState
            .modifyGameStateAbility(creatureId, abilityId) { ability =>
              ability
                .modify(_.components.at(componentId).params.channelTimer)
                .using(_.restart())
                .modify(_.components.at(componentId).params.state)
                .setTo(AbilityState.Channel)
            }
            .onAbilityComponentChannelStart(creatureId, abilityId, componentId)
        })
        .modifyGameStateCreature(creatureId)(
          _.modify(_.params.staminaRegenerationDisabledTimer)
            .using(_.restart())
            .modify(_.params.isStaminaRegenerationDisabled)
            .setTo(true)
            .takeStaminaDamage(15f)
        )

    } else this
  }
}
