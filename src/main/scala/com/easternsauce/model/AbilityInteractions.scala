package com.easternsauce.model

import com.easternsauce.model.creature.ability.{Ability, AbilityState}
import com.softwaremill.quicklens._

import scala.util.chaining._

trait AbilityInteractions {
  this: GameState =>

  def onAbilityActiveStart(creatureId: String, abilityId: String): GameState = {

    this.modifyCreature(creatureId) { creature =>
      val restartTimers: Ability => Ability = _.modify(_.params.abilityActiveAnimationTimer)
        .using(_.restart())
        .modify(_.params.activeTimer)
        .using(_.restart())
      val setDirVector: Ability => Ability = _.modify(_.params.dirVector).setTo(creature.params.dirVector)

      creature
        .modifyAbility(abilityId)(restartTimers)
        .takeStaminaDamage(15f)
        .modifyAbility(abilityId)(setDirVector)
        .modifyAbility(abilityId)(_.updateHitbox(creature))

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
              .modifyAbility(creatureId, abilityId) {
                _.onStop()
                  .modify(_.params.state)
                  .setTo(AbilityState.Inactive)
              }
              //.updateHitbox ??
              .onAbilityActiveUpdate(creatureId, abilityId)

          case state => state
        }
      case Inactive =>
        this.pipe(
          state =>
            if (ability.params.onCooldown && activeTimer.time > ability.cooldownTime)
              modifyAbility(creatureId, abilityId)(_.modify(_.params.onCooldown).setTo(false))
            else state
        )
      case _ => this
    }
  }
}
