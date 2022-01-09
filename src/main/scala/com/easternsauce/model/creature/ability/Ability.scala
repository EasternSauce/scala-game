package com.easternsauce.model.creature.ability

import com.badlogic.gdx.graphics.g2d.{Animation, TextureRegion}
import com.badlogic.gdx.math.Vector2
import com.easternsauce.model.creature.Creature
import com.easternsauce.util.{Constants, Vector2Wrapper}
import com.softwaremill.quicklens.ModifyPimp

case class Ability(params: AbilityParams) {
  val activeAnimation: Option[Animation[TextureRegion]] = None
  val channelAnimation: Option[Animation[TextureRegion]] = None

  val textureWidth: Int = 0
  val textureHeight: Int = 0

  val totalActiveTime: Float = 0f
  val totalChannelTime: Float = 0f

  val cooldownTime: Float = 0f

  val isAttack = false

  val damage = 0f

  val channelSpriteType: String = ""
  val activeSpriteType: String = ""
  val channelFrameCount: Int = 0
  val activeFrameCount: Int = 0
  val channelFrameDuration: Float = 0
  val activeFrameDuration: Float = 0

  protected def width: Float = textureWidth.toFloat / Constants.PPM

  protected def height: Float = textureHeight.toFloat / Constants.PPM

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

  def setDirVector(dirVector: Vector2Wrapper): Ability = this.modify(_.params.dirVector).setTo(dirVector)

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

}

