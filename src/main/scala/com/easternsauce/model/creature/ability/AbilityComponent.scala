package com.easternsauce.model.creature.ability

import com.badlogic.gdx.graphics.g2d.{Animation, TextureRegion}
import com.easternsauce.util.Constants
import com.softwaremill.quicklens.ModifyPimp

case class AbilityComponent(params: AbilityComponentParams) {
  val activeAnimation: Option[Animation[TextureRegion]] = None
  val channelAnimation: Option[Animation[TextureRegion]] = None

  val textureWidth: Int = 40
  val textureHeight: Int = 40

  val totalActiveTime: Float = 0.3f
  val totalChannelTime: Float = 0.3f

  val cooldownTime: Float = 0.8f

  val channelSpriteType: String = "slash_windup"
  val activeSpriteType: String = "slash"
  val channelFrameCount: Int = 6
  val activeFrameCount: Int = 6
  val channelFrameDuration: Float = 0.05f
  val activeFrameDuration: Float = 0.05f

  val isAttack: Boolean = true

  val damage: Float = 35f

  def width: Float = textureWidth.toFloat / Constants.PPM

  def height: Float = textureHeight.toFloat / Constants.PPM

  def makeInactive(): AbilityComponent = this.modify(_.params.state).setTo(AbilityState.Inactive)

  def makeActive(): AbilityComponent = this.modify(_.params.state).setTo(AbilityState.Active)

  def stop(): AbilityComponent = {
    this // TODO?
  }

}
