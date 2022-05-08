package com.easternsauce.model.creature.ability

import com.easternsauce.util.{Constants, Vector2Wrapper}
import com.softwaremill.quicklens.ModifyPimp

case class AbilityComponent(specification: AbilitySpecification, params: ComponentParams) {

  def isAttack: Boolean = true

  def damage: Float = 35f // TODO

  def speed: Float = specification.initSpeed

  def scale: Float = specification.scale

  def width: Float = specification.textureWidth.toFloat * specification.scale / Constants.PPM

  def height: Float = specification.textureHeight.toFloat * specification.scale / Constants.PPM

  def makeInactive(): AbilityComponent = this.modify(_.params.state).setTo(AbilityState.Inactive)

  def makeActive(): AbilityComponent = this.modify(_.params.state).setTo(AbilityState.Active)

  def stop(): AbilityComponent = {
    this.makeInactive()
  }

  def setDirVector(dirVector: Vector2Wrapper): AbilityComponent = {
    this.modify(_.params.dirVector).setTo(dirVector.rotate(params.angleDeviation))
  }

}
