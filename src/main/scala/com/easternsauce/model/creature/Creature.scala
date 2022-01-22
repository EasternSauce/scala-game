package com.easternsauce.model.creature

import com.easternsauce.model.creature.ability.{Ability, AbilityComponent, TestProjectile}
import com.easternsauce.util.Direction.Direction
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
  val dirMap: Map[Direction, Int]
  val baseLife: Float

  protected val staminaRegenerationTickTime = 0.005f
  protected val staminaRegeneration = 0.8f
  protected val staminaOveruseTime = 2f
  protected val staminaRegenerationDisabled = 1.2f

  def init(): Creature = {
    this.modify(_.params.abilities).setTo(Map("regularAttack" -> TestProjectile().init()))
  }

  def update(delta: Float): Creature = {
    this
      .updateTimers(delta)
      .updateStamina(delta)
  }

  def updateTimers(delta: Float): Creature = {
    this
      .modify(_.params.animationTimer)
      .using(_.update(delta))
      .modify(_.params.staminaOveruseTimer)
      .using(_.update(delta))
      .modify(_.params.staminaRegenerationTimer)
      .using(_.update(delta))
      .modify(_.params.staminaRegenerationDisabledTimer)
      .using(_.update(delta))
  }

  def setPosition(newPosX: Float, newPosY: Float): Creature = {
    this
      .modify(_.params.posX)
      .setTo(newPosX)
      .modify(_.params.posY)
      .setTo(newPosY)
  }

  def takeStaminaDamage(staminaDamage: Float): Creature = {
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
  }

  def modifyAbility(abilityId: String)(operation: Ability => Ability): Creature =
    this
      .modify(_.params.abilities.at(abilityId))
      .using(operation)

  def modifyAbilityComponent(abilityId: String, componentId: String)(
    operation: AbilityComponent => AbilityComponent
  ): Creature =
    this
      .modify(_.params.abilities.at(abilityId).components.at(componentId))
      .using(operation)

  def updateStamina(delta: Float): Creature = {
    this
      .pipe(
        creature =>
          if (creature.params.isSprinting && creature.params.stamina > 0) {
            creature.modify(_.params.staminaDrainTimer).using(_.update(delta))
          } else creature
      )
      .pipe(
        creature =>
          if (!params.isStaminaRegenerationDisabled && !creature.params.isSprinting) {
            if (
              creature.params.staminaRegenerationTimer.time > creature.staminaRegenerationTickTime /* && !abilityActive */ && !creature.params.staminaOveruse
            ) {
              creature
                .pipe(creature => {
                  val afterRegeneration = creature.params.stamina + creature.staminaRegeneration
                  creature
                    .modify(_.params.stamina)
                    .setToIf(creature.params.stamina < creature.params.maxStamina)(
                      Math.min(afterRegeneration, creature.params.maxStamina)
                    )
                })
                .modify(_.params.staminaRegenerationTimer)
                .using(_.restart())
            } else creature

          } else creature
      )
      .pipe(
        creature =>
          creature
            .modify(_.params.staminaOveruse)
            .setToIf(
              creature.params.staminaOveruse && creature.params.staminaOveruseTimer.time > creature.staminaOveruseTime
            )(false)
      )
      .pipe(
        _.modify(_.params.isStaminaRegenerationDisabled)
          .setToIf(params.staminaRegenerationDisabledTimer.time > staminaRegenerationDisabled)(false)
      )
  }

  def onDeath(): Creature = {
    this
  }

  def isAlive: Boolean = params.life > 0f

  def copy(params: CreatureParams = params): Creature

}
