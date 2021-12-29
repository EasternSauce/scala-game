package com.easternsauce.model

import com.easternsauce.model.creature.ability.AbilityState
import com.easternsauce.model.event.{AbilityCreateBodyEvent, AbilityDestroyBodyEvent}
import com.softwaremill.quicklens.ModifyPimp

import scala.util.chaining._

trait AbilityInteractions {
  this: GameState =>

  def onCreatureAbilityActiveStart(creatureId: String, abilityId: String): GameState = {

    this
      .pipe(
        gameState => gameState.modify(_.events).setTo(AbilityCreateBodyEvent(creatureId, abilityId) :: gameState.events)
      )
      .modifyGameStateCreature(creatureId) { creature =>
        creature
          .modifyCreatureAbility(abilityId)(_.restartActiveTimers())
          .modifyCreatureAbility(abilityId)(_.modify(_.params.channelTimer).using(_.stop()))
          .modifyCreatureAbility(abilityId)(_.modify(_.params.abilityChannelAnimationTimer).using(_.stop()))
          .takeStaminaDamage(15f)
          .modifyCreatureAbility(abilityId)(_.setDirVector(creature.params.dirVector))
          .modifyCreatureAbility(abilityId)(_.updateHitbox(creature))
      }
  }

  def onCreatureAbilityChannelStart(creatureId: String, abilityId: String): GameState = {
    this
      .modifyGameStateCreature(creatureId) { creature =>
        creature
          .modifyCreatureAbility(abilityId)(_.modify(_.params.channelTimer).using(_.restart()))
          .modifyCreatureAbility(abilityId)(_.modify(_.params.abilityChannelAnimationTimer).using(_.restart()))
          .modifyCreatureAbility(abilityId)(_.setDirVector(creature.params.dirVector))
          .modifyCreatureAbility(abilityId)(_.updateHitbox(creature))
      }

  }

  def onCreatureAbilityInactiveStart(creatureId: String, abilityId: String): GameState = {
    this
      .pipe(
        gameState =>
          gameState.modify(_.events).setTo(AbilityDestroyBodyEvent(creatureId, abilityId) :: gameState.events)
      )
      .modifyGameStateCreature(creatureId) { creature =>
        creature
          .modifyCreatureAbility(abilityId)(_.modify(_.params.activeTimer).using(_.stop()))
          .modifyCreatureAbility(abilityId)(_.modify(_.params.abilityActiveAnimationTimer).using(_.stop()))
      }
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

    val channelTimer = ability.params.channelTimer
    val activeTimer = ability.params.activeTimer

    import com.easternsauce.model.creature.ability.AbilityState._
    (ability.params.state match {
      case Channel =>
        this
          .pipe {
            case state if channelTimer.time > ability.totalChannelTime =>
              state
                .modifyGameStateAbility(creatureId, abilityId)(_.stop().makeActive())
                .onCreatureAbilityActiveStart(creatureId, abilityId)
            case state => state
          }
          .onCreatureAbilityChannelUpdate(creatureId, abilityId)
      case Active =>
        this
          .pipe {
            case state if activeTimer.time > ability.totalActiveTime =>
              state
                .modifyGameStateAbility(creatureId, abilityId)(_.stop().makeInactive())
                .onCreatureAbilityInactiveStart(creatureId, abilityId)

            case state => state
          }
          .onCreatureAbilityActiveUpdate(creatureId, abilityId)
      case Inactive =>
        this.pipe(
          state =>
            if (ability.params.onCooldown && activeTimer.time > ability.cooldownTime)
              modifyGameStateAbility(creatureId, abilityId)(_.setNotOnCooldown())
            else state
        )
      case _ => this
    }).modifyGameStateAbility(creatureId, abilityId)(_.updateTimers(delta))
  }

  def performAbility(creatureId: String, abilityId: String): GameState = {
    val creature = creatures(creatureId)

    val ability = creature.params.abilities(abilityId)

    if (
      creature.params.stamina > 0 && ability.params.state == AbilityState.Inactive && !ability.params.onCooldown
      /*&& !creature.abilityActive*/
    ) {
      this
        .modifyGameStateAbility(creatureId, abilityId) { ability =>
          ability
            .modify(_.params.channelTimer)
            .using(_.restart())
            .modify(_.params.state)
            .setTo(AbilityState.Channel)
        }
        .onCreatureAbilityChannelStart(creatureId, abilityId)
        .modifyGameStateCreature(creatureId)(
          _.modify(_.params.staminaRegenerationDisabledTimer)
            .using(_.restart())
            .modify(_.params.isStaminaRegenerationDisabled)
            .setTo(true)
        )
    } else this

  }
}
