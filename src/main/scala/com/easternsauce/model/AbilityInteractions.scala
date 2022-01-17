package com.easternsauce.model

import com.easternsauce.model.creature.ability.AbilityState
import com.easternsauce.model.event.{AbilityCreateBodyEvent, AbilityDestroyBodyEvent}
import com.easternsauce.util.Vector2Wrapper
import com.softwaremill.quicklens._

import scala.util.chaining._

trait AbilityInteractions {
  this: GameState =>

  def onAbilityComponentActiveStart(creatureId: String, abilityId: String, componentId: String): GameState = {

    val ability = this.abilities(creatureId, abilityId)

    val creature = this.creatures(creatureId)

    this
      .pipe(
        gameState => gameState.modify(_.events).setTo(AbilityCreateBodyEvent(creatureId, abilityId) :: gameState.events)
      )
      .modifyGameStateAbilityComponent(creatureId, abilityId, componentId) {
        _.modify(_.params.abilityActiveAnimationTimer)
          .using(_.restart())
          .modify(_.params.activeTimer)
          .using(_.restart())
          .modify(_.params.channelTimer)
          .using(_.stop())
          .modify(_.params.abilityChannelAnimationTimer)
          .using(_.stop())
          .setDirVector(Vector2Wrapper(creature.params.dirVector.x, creature.params.dirVector.y))
          .pipe(ability.updateComponentHitbox(creature, _))
      }
  }

  def onAbilityComponentChannelStart(creatureId: String, abilityId: String, componentId: String): GameState = {

    val ability = this.abilities(creatureId, abilityId)
    val creature = this.creatures(creatureId)

    this
      .modifyGameStateAbilityComponent(creatureId, abilityId, componentId) {
        _.modify(_.params.channelTimer)
          .using(_.restart())
          .modify(_.params.abilityChannelAnimationTimer)
          .using(_.restart())
          .setDirVector(Vector2Wrapper(creature.params.dirVector.x, creature.params.dirVector.y))
          .pipe(ability.updateComponentHitbox(creature, _))
      }
      .modifyGameStateAbility(creatureId, abilityId)(_.modify(_.params.abilityTimer).using(_.restart()))

  }

  def onAbilityComponentInactiveStart(creatureId: String, abilityId: String, componentId: String): GameState = {

    this
      .pipe(
        gameState =>
          gameState.modify(_.events).setTo(AbilityDestroyBodyEvent(creatureId, abilityId) :: gameState.events)
      )
      .modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(
        _.modify(_.params.activeTimer)
          .using(_.stop())
          .modify(_.params.abilityActiveAnimationTimer)
          .using(_.stop())
      )

  }

  def onAbilityComponentChannelUpdate(creatureId: String, abilityId: String, componentId: String): GameState = {
    val ability = abilities(creatureId, abilityId)
    val creature = this.creatures(creatureId)

    this.modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(ability.updateComponentHitbox(creature, _))
  }

  def onAbilityComponentActiveUpdate(creatureId: String, abilityId: String, componentId: String): GameState = {
    val ability = abilities(creatureId, abilityId)
    val creature = this.creatures(creatureId)

    this.modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(ability.updateComponentHitbox(creature, _))

  }

  def updateCreatureAbility(creatureId: String, abilityId: String, delta: Float): GameState = {
    val ability = abilities(creatureId, abilityId)

    ability.components.keys
      .foldLeft(this)((gameState, componentId) => {
        val channelTimer = ability.components(componentId).params.channelTimer
        val activeTimer = ability.components(componentId).params.activeTimer

        import com.easternsauce.model.creature.ability.AbilityState._
        (ability.components(componentId).params.state match {
          case Channel =>
            gameState
              .pipe {
                case state if channelTimer.time > ability.components(componentId).specification.totalChannelTime =>
                  state
                    .modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(_.stop().makeActive())
                    .onAbilityComponentActiveStart(creatureId, abilityId, componentId)
                case state => state
              }
              .onAbilityComponentChannelUpdate(creatureId, abilityId, componentId)
          case Active =>
            gameState
              .pipe {
                case state if activeTimer.time > ability.components(componentId).specification.totalActiveTime =>
                  state
                    .modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(_.stop().makeInactive())
                    .onAbilityComponentInactiveStart(creatureId, abilityId, componentId)

                case state => state
              }
              .onAbilityComponentActiveUpdate(creatureId, abilityId, componentId)
          case Inactive =>
            gameState // TODO do we need update anything when inactive?
          case _ => gameState
        }).modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(ability.updateComponentTimers(_, delta))
      })
      .modifyGameStateAbility(creatureId, abilityId)(_.updateTimers(delta))

  }

  def performAbility(creatureId: String, abilityId: String): GameState = {
    val creature = creatures(creatureId)

    val ability = creature.params.abilities(abilityId)

    if (
      creature.params.stamina > 0 && !ability.componentsActive && !ability.onCooldown
      /*&& !creature.abilityActive*/
    ) {
      ability.components.keys
        .foldLeft(this)((gameState, componentId) => {
          gameState
            .modifyGameStateAbilityComponent(creatureId, abilityId, componentId) {
              _.modify(_.params.channelTimer)
                .using(_.restart())
                .modify(_.params.state)
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
