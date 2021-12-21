package com.easternsauce.model

import com.easternsauce.model.creature.ability.AbilityState
import com.softwaremill.quicklens.ModifyPimp

import scala.util.chaining._

trait AbilityInteractions {
  this: GameState =>

  def onCreatureAbilityActiveStart(creatureId: String, abilityId: String): GameState = {

    println("active start")

    this.modifyGameStateCreature(creatureId) { creature =>
      creature
        .modifyCreatureAbility(abilityId)(_.restartActiveTimers())
        .modifyCreatureAbility(abilityId)(_.modify(_.params.channelTimer).using(_.stop()))
        .modifyCreatureAbility(abilityId)(_.modify(_.params.abilityChannelAnimationTimer).using(_.stop()))
        .takeStaminaDamage(15f)
        .modifyCreatureAbility(abilityId)(_.setDirVector(creature.params.dirVector))
        .modifyCreatureAbility(abilityId)(_.updateHitbox(creature))
    }
  }

  def onCreatureAbilityChannelingStart(creatureId: String, abilityId: String): GameState = {

    println("channeling start")

    this.modifyGameStateCreature(creatureId) { creature =>
      creature
        .modifyCreatureAbility(abilityId)(_.modify(_.params.channelTimer).using(_.restart()))
        .modifyCreatureAbility(abilityId)(_.modify(_.params.abilityChannelAnimationTimer).using(_.restart()))
        .modifyCreatureAbility(abilityId)(_.setDirVector(creature.params.dirVector))
        .modifyCreatureAbility(abilityId)(_.updateHitbox(creature))
    }

  }

  def onCreatureAbilityInactiveStart(creatureId: String, abilityId: String): GameState = {

    this.modifyGameStateCreature(creatureId) { creature =>
      creature
        .modifyCreatureAbility(abilityId)(_.modify(_.params.activeTimer).using(_.stop()))
        .modifyCreatureAbility(abilityId)(_.modify(_.params.abilityActiveAnimationTimer).using(_.stop()))
    }
  }

    def onCreatureAbilityChannelingUpdate(creatureId: String, abilityId: String): GameState = this

  def onCreatureAbilityActiveUpdate(creatureId: String, abilityId: String): GameState = {
    println("active update")
    this
  }

  def updateCreatureAbility(creatureId: String, abilityId: String, delta: Float): GameState = {
    //val creature = creatures(creatureId)
    val ability = abilities(creatureId, abilityId)

//    if (creatureId == "player") {
//      println(
//        "updating ability " + creatureId + " " + abilityId + " state: " + ability.params.state + " channel time: " + ability.params.channelTimer.time
//      )
//    }

    val channelTimer = ability.params.channelTimer
    val activeTimer = ability.params.activeTimer

    import com.easternsauce.model.creature.ability.AbilityState._
    (ability.params.state match {
      case Channeling =>
        this
          .pipe {
            case state if channelTimer.time > ability.totalChannelTime =>
              state
                .modifyGameStateAbility(creatureId, abilityId)(_.stop().makeActive())
                .onCreatureAbilityActiveStart(creatureId, abilityId)
            case state => state
          }
          //.updateHitbox ??
          .onCreatureAbilityChannelingUpdate(creatureId, abilityId)
      case Active =>
        this.pipe {
          case state if activeTimer.time > ability.totalActiveTime =>
            state
              .modifyGameStateAbility(creatureId, abilityId)(_.stop().makeInactive())
              .onCreatureAbilityInactiveStart(creatureId, abilityId)

          //.updateHitbox ??


          case state => state
        }.onCreatureAbilityActiveUpdate(creatureId, abilityId)
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

    //    println("performing ability")
    val ability = creature.params.abilities(abilityId)
    //val channelTimer = ability.params.channelTimer

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
            .setTo(AbilityState.Channeling)
        }
        .onCreatureAbilityChannelingStart(creatureId, abilityId)
    } else this

  }
}
