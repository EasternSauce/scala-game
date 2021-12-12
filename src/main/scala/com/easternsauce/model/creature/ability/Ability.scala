package com.easternsauce.model.creature.ability

import com.badlogic.gdx.graphics.g2d.{Animation, TextureRegion}
import com.easternsauce.model.creature.Creature
import com.easternsauce.util.Constants

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

  def onStop(): Ability = ???

  def updateHitbox(creature: Creature): Ability = this

  def copy(params: AbilityParams = params): Ability

}
