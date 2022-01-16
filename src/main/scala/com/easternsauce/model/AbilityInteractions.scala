package com.easternsauce.model

import com.easternsauce.model.creature.ability.AbilityState
import com.easternsauce.model.event.{AbilityCreateBodyEvent, AbilityDestroyBodyEvent}
import com.easternsauce.util.Vector2Wrapper
import com.softwaremill.quicklens._

import scala.util.chaining._

trait AbilityInteractions {
  this: GameState =>

  def onCreatureAbilityActiveStart(creatureId: String, abilityId: String, componentId: String): GameState = {

    this
      .pipe(
        gameState => gameState.modify(_.events).setTo(AbilityCreateBodyEvent(creatureId, abilityId) :: gameState.events)
      )
      .modifyGameStateCreature(creatureId) { creature =>
        creature
          .modifyCreatureAbility(abilityId)(_.restartActiveTimers())
          .modifyCreatureAbility(abilityId)(_.modify(_.components.at(componentId).params.channelTimer).using(_.stop()))
          .modifyCreatureAbility(abilityId)(
            _.modify(_.components.at(componentId).params.abilityChannelAnimationTimer).using(_.stop())
          )
          .takeStaminaDamage(15f)
          .modifyCreatureAbility(abilityId)(
            _.setDirVector(Vector2Wrapper(creature.params.dirVector.x, creature.params.dirVector.y))
          )
          .modifyCreatureAbility(abilityId)(_.updateHitbox(creature))
      }
  }

  def onCreatureAbilityChannelStart(creatureId: String, abilityId: String): GameState = {
    val ability = abilities(creatureId, abilityId)

    ability.components.keys.foldLeft(this)((gameState, componentId) => {

      gameState
        .modifyGameStateCreature(creatureId) { creature =>
          creature
            .modifyCreatureAbility(abilityId)(
              _.modify(_.components.at(componentId).params.channelTimer).using(_.restart())
            )
            .modifyCreatureAbility(abilityId)(
              _.modify(_.components.at(componentId).params.abilityChannelAnimationTimer).using(_.restart())
            )
            .modifyCreatureAbility(abilityId)(
              _.setDirVector(Vector2Wrapper(creature.params.dirVector.x, creature.params.dirVector.y))
            )
            .modifyCreatureAbility(abilityId)(_.updateHitbox(creature))
        }
    })
  }

  def onCreatureAbilityInactiveStart(creatureId: String, abilityId: String): GameState = {
    val ability = abilities(creatureId, abilityId)

    ability.components.keys.foldLeft(this)((gameState, componentId) => {
      gameState
        .pipe(
          gameState =>
            gameState.modify(_.events).setTo(AbilityDestroyBodyEvent(creatureId, abilityId) :: gameState.events)
        )
        .modifyGameStateCreature(creatureId) { creature =>
          creature
            .modifyCreatureAbility(abilityId)(_.modify(_.components.at(componentId).params.activeTimer).using(_.stop()))
            .modifyCreatureAbility(abilityId)(
              _.modify(_.components.at(componentId).params.abilityActiveAnimationTimer).using(_.stop())
            )
        }
    })

  }

  def onCreatureAbilityChannelUpdate(creatureId: String, abilityId: String): GameState =
    this.modifyGameStateCreature(creatureId)(
      creature => creature.modifyCreatureAbility(abilityId)(_.updateHitbox(creature))
    )

  def onCreatureAbilityActiveUpdate(creatureId: String, abilityId: String): GameState = {
    this.modifyGameStateCreature(creatureId)(
      creature => creature.modifyCreatureAbility(abilityId)(_.updateHitbox(creature))
    )
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
              case state if channelTimer.time > ability.components(componentId).totalChannelTime =>
                state
                  .modifyGameStateAbility(creatureId, abilityId)(
                    _.stop().modify(_.components.at(componentId)).using(_.makeActive())
                  )
                  .onCreatureAbilityActiveStart(creatureId, abilityId, componentId)
              case state => state
            }
            .onCreatureAbilityChannelUpdate(creatureId, abilityId)
        case Active =>
          gameState
            .pipe {
              case state if activeTimer.time > ability.components(componentId).totalActiveTime =>
                state
                  .modifyGameStateAbility(creatureId, abilityId)(
                    _.stop().modify(_.components.at(componentId)).using(_.makeInactive())
                  )
                  .onCreatureAbilityInactiveStart(creatureId, abilityId)

              case state => state
            }
            .onCreatureAbilityActiveUpdate(creatureId, abilityId)
        case Inactive =>
          gameState.pipe(
            state =>
              if (ability.params.onCooldown && activeTimer.time > ability.components(componentId).cooldownTime)
                modifyGameStateAbility(creatureId, abilityId)(_.setNotOnCooldown())
              else state
          )
        case _ => gameState
      }).modifyGameStateAbility(creatureId, abilityId)(_.updateTimers(delta))
    })

  }

  def performAbility(creatureId: String, abilityId: String): GameState = {
    val creature = creatures(creatureId)

    val ability = creature.params.abilities(abilityId)

    ability.components.keys.foldLeft(this)((gameState, componentId) => {

      if (
        creature.params.stamina > 0 && ability
          .components(componentId)
          .params
          .state == AbilityState.Inactive && !ability.params.onCooldown
        /*&& !creature.abilityActive*/
      ) {
        gameState
          .modifyGameStateAbility(creatureId, abilityId) { ability =>
            ability
              .modify(_.components.at(componentId).params.channelTimer)
              .using(_.restart())
              .modify(_.components.at(componentId).params.state)
              .setTo(AbilityState.Channel)
          }
          .onCreatureAbilityChannelStart(creatureId, abilityId)
          .modifyGameStateCreature(creatureId)(
            _.modify(_.params.staminaRegenerationDisabledTimer)
              .using(_.restart())
              .modify(_.params.isStaminaRegenerationDisabled)
              .setTo(true)
          )
      } else gameState

    })
  }
}
