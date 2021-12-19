package com.easternsauce.model.creature

import com.easternsauce.model.creature.ability.{Ability, AbilityState}
import com.easternsauce.util.Direction
import com.softwaremill.quicklens._

import scala.util.chaining.scalaUtilChainingOps

abstract class Creature {
  val isPlayer = false

  val params: CreatureParams
  val spriteType: String
  val textureWidth: Int
  val textureHeight: Int
  val width: Float
  val height: Float
  val frameDuration: Float
  val frameCount: Int
  val neutralStanceFrame: Int
  val dirMap: Map[Direction.Value, Int]

  def update(delta: Float): Creature = {
    this.updateTimers(delta)
  }

  def updateTimers(delta: Float): Creature = {
    this
      .modify(_.params.animationTimer)
      .using(_.update(delta))
      .modify(_.params.staminaOveruseTimer)
      .using(_.update(delta))
  }

  def setPosition(newPosX: Float, newPosY: Float): Creature = {
    this
      .modify(_.params.posX)
      .setTo(newPosX)
      .modify(_.params.posY)
      .setTo(newPosY)
  }

  def takeStaminaDamage(staminaDamage: Float): Creature =
    if (params.stamina - staminaDamage > 0) this.modify(_.params.stamina).setTo(this.params.stamina - staminaDamage)
    else {
      this
        .modify(_.params.stamina)
        .setTo(0f)
        .modify(_.params.staminaOveruse)
        .setTo(true)
        .modify(_.params.staminaOveruseTimer)
        .using(_.restart())
    }

  def modifyCreatureAbility(abilityId: String)(operation: Ability => Ability): Creature =
    this
      .modify(_.params.abilities.at(abilityId))
      .using(operation)

  def performAbility(abilityId: String): Creature = {
    println("performing ability")
    val ability = params.abilities(abilityId)
    //val channelTimer = ability.params.channelTimer

    this.pipe(
      creature =>
        if (
          creature.params.stamina > 0 && ability.params.state == AbilityState.Inactive && !ability.params.onCooldown
          /*&& !creature.abilityActive*/
        )
          this.modifyCreatureAbility(abilityId)(
            _.modify(_.params.channelTimer).using(_.restart()).modify(_.params.state).setTo(AbilityState.Channeling)
          )
        else creature
    )

  }

  def copy(params: CreatureParams = params): Creature

}

object Creature {}
