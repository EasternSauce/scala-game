package com.easternsauce.model.creature.ability

import com.badlogic.gdx.math.Vector2
import com.easternsauce.model.creature.Creature
import com.softwaremill.quicklens.ModifyPimp

object RegularAttack {
  def apply(params: AbilityParams = AbilityParams()): Ability = new Ability(params) {
    override val textureWidth: Int = 40
    override val textureHeight: Int = 40

    override val totalActiveTime: Float = 0.3f
    override val totalChannelTime: Float = 0.3f

    override val cooldownTime: Float = 0.8f

    override val channelSpriteType: String = "slash_windup"
    override val activeSpriteType: String = "slash"
    override val channelFrameCount: Int = 6
    override val activeFrameCount: Int = 6
    override val channelFrameDuration: Float = 0.05f
    override val activeFrameDuration: Float = 0.05f

    override val isAttack: Boolean = true

    override val damage: Float = 35f

    override def updateHitbox(creature: Creature): Ability = {
      val theta = new Vector2(params.dirVector.x, params.dirVector.y).angleDeg()

      val attackShiftX = params.dirVector.normal().x * params.attackRange
      val attackShiftY = params.dirVector.normal().y * params.attackRange

      val attackRectX = attackShiftX + creature.params.posX
      val attackRectY = attackShiftY + creature.params.posY

      val result: Ability = this
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
      result
    }
  }
}