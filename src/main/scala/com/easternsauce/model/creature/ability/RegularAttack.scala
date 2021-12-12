package com.easternsauce.model.creature.ability

import com.badlogic.gdx.math.Vector2
import com.easternsauce.model.creature.Creature
import com.softwaremill.quicklens.ModifyPimp

case class RegularAttack(override val params: AbilityParams = AbilityParams()) extends Ability(params = params) {

  override val spriteWidth: Int = 40
  override val spriteHeight: Int = 40

  override val totalActiveTime: Float = 0.3f
  override val totalChannelTime: Float = 0.3f

  override val cooldownTime: Float = 0.8f

  override def updateHitbox(creature: Creature): Ability = {
    val theta = new Vector2(params.dirVector.x, params.dirVector.y).angleDeg()
    val attackShiftX = params.dirVector.nor().x * params.attackRange.get
    val attackShiftY = params.dirVector.nor().y * params.attackRange.get

    val attackRectX = attackShiftX + creature.params.posX
    val attackRectY = attackShiftY + creature.params.posY

    this
      .modify(_.params.abilityHitbox)
      .setTo(
        AbilityHitbox(
          x = attackRectX,
          y = attackRectY,
          width = width,
          height = height,
          rotationAngle = theta,
          scale = scale
        )
      )
  }

  def copy(params: AbilityParams = params): RegularAttack = RegularAttack(params)

}
