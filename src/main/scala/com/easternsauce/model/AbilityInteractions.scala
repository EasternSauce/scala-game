package com.easternsauce.model

import scala.util.chaining._

trait AbilityInteractions {
  this: GameState =>

  def onAbilityActiveStart(creatureId: String, abilityId: String): GameState = {

    this.modifyGameStateCreature(creatureId) { creature =>
      creature
        .modifyCreatureAbility(abilityId)(_.restartActiveTimers())
        .takeStaminaDamage(15f)
        .modifyCreatureAbility(abilityId)(_.setDirVector(creature.params.dirVector))
        .modifyCreatureAbility(abilityId)(_.updateHitbox(creature))
    }
  }

  def onAbilityChannelingUpdate(creatureId: String, abilityId: String): GameState = ???

  def onAbilityActiveUpdate(creatureId: String, abilityId: String): GameState = ???

  def updateAbility(creatureId: String, abilityId: String): GameState = {
    val creature = creatures(creatureId)
    val ability = abilities(creatureId, abilityId)

    val channelTimer = ability.params.channelTimer
    val activeTimer = ability.params.activeTimer

    import com.easternsauce.model.creature.ability.AbilityState._
    ability.params.state match {
      case Channeling =>
        this
          .pipe {
            case state if channelTimer.time > ability.totalChannelTime =>
              state.onAbilityActiveStart(creatureId, abilityId)
            case state => state
          }
          //.updateHitbox ??
          .onAbilityChannelingUpdate(creatureId, abilityId)
      case Active =>
        this.pipe {
          case state if activeTimer.time > ability.totalActiveTime =>
            state
              .modifyGameStateAbility(creatureId, abilityId)(_.stop().makeInactive())
              //.updateHitbox ??
              .onAbilityActiveUpdate(creatureId, abilityId)

          case state => state
        }
      case Inactive =>
        this.pipe(
          state =>
            if (ability.params.onCooldown && activeTimer.time > ability.cooldownTime)
              modifyGameStateAbility(creatureId, abilityId)(_.setNotOnCooldown())
            else state
        )
      case _ => this
    }
  }
}
