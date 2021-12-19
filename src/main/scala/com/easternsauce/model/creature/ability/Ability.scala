package com.easternsauce.model.creature.ability

import com.badlogic.gdx.graphics.g2d.{Animation, TextureRegion}
import com.badlogic.gdx.math.Vector2
import com.easternsauce.model.creature.Creature
import com.easternsauce.util.Constants
import com.softwaremill.quicklens.ModifyPimp

abstract class Ability(val params: AbilityParams) {

  val activeAnimation: Option[Animation[TextureRegion]] = None
  val channelAnimation: Option[Animation[TextureRegion]] = None

  val spriteWidth: Int = 0
  val spriteHeight: Int = 0

  val totalActiveTime: Float = 0f
  val totalChannelTime: Float = 0f

  val cooldownTime: Float = 0f

  protected def width: Float = spriteWidth.toFloat / Constants.PPM

  protected def height: Float = spriteHeight.toFloat / Constants.PPM

  def scale: Float = {
    //if (creature.isWeaponEquipped) creature.currentWeapon.template.attackScale.get
    //else
    1.4f
  }

  def stop(): Ability = {
    this // TODO?
  }

  def makeInactive(): Ability = this.modify(_.params.state).setTo(AbilityState.Inactive)

  def makeActive(): Ability = this.modify(_.params.state).setTo(AbilityState.Active)

  def setNotOnCooldown(): Ability = this.modify(_.params.onCooldown).setTo(false)

  def setDirVector(dirVector: Vector2): Ability = this.modify(_.params.dirVector).setTo(dirVector)

  def updateHitbox(creature: Creature): Ability = this

  def restartActiveTimers(): Ability =
    this
      .modify(_.params.abilityActiveAnimationTimer)
      .using(_.restart())
      .modify(_.params.activeTimer)
      .using(_.restart())

  def updateTimers(delta: Float): Ability = {
    this
      .modify(_.params.activeTimer)
      .using(_.update(delta))
      .modify(_.params.channelTimer)
      .using(_.update(delta))
      .modify(_.params.abilityChannelAnimationTimer)
      .using(_.update(delta))
      .modify(_.params.abilityActiveAnimationTimer)
      .using(_.update(delta))
  }

  def copy(params: AbilityParams = params): Ability

}
