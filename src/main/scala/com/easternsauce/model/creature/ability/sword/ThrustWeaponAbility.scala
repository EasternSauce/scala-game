package com.easternsauce.model.creature.ability.sword

import com.badlogic.gdx.math.Vector2
import com.easternsauce.model.GameState
import com.easternsauce.model.creature.Creature
import com.easternsauce.model.creature.ability._
import com.easternsauce.util.Vec2
import com.softwaremill.quicklens._

case class ThrustWeaponAbility(
  override val params: AbilityParams = AbilityParams(id = "thrustWeapon"),
  override val components: Map[String, AbilityComponent] = Map()
) extends Ability(params = params, components = components) {

  override val specification: Option[AbilitySpecification] = Some(
    AbilitySpecification(
      textureWidth = 64,
      textureHeight = 32,
      totalActiveTime = 0.275f,
      totalChannelTime = 0.595f,
      channelSpriteType = "trident_thrust_windup",
      activeSpriteType = "trident_thrust",
      channelFrameCount = 7,
      activeFrameCount = 11,
      channelFrameDuration = 0.085f,
      activeFrameDuration = 0.025f,
      componentType = ComponentType.MeleeAttack,
      scale = 1.4f,
      initSpeed = 30f
    )
  )

  override val abilityActiveSoundId: Option[String] = Some("swoosh")

  override val isWeaponAttack = true

  override def onStart(creatureId: String): GameState => GameState = { gameState =>
    val creature = gameState.creatures(creatureId)
    gameState.modifyEachAbilityComponent(creatureId, abilityId) { (ability, componentId) =>
      val component = components(componentId)
      val dirVector = Vec2(creature.params.actionDirVector.x, creature.params.actionDirVector.y)
      val theta = dirVector.angleDeg() + component.params.angleDeviation

      ability
        .modify(_.components.at(componentId).params.abilityHitbox)
        .setTo(
          AbilityHitbox(
            x = creature.params.posX,
            y = creature.params.posY,
            width = component.width,
            height = component.height,
            rotationAngle = theta,
            scale = component.scale
          )
        )
        .modify(_.components.at(componentId).params.renderPos)
        .setTo(Vec2(x = creature.params.posX, y = creature.params.posY))
        .modify(_.components.at(componentId))
        .using(_.setDirVector(dirVector))
        .modify(_.components.at(componentId).params.renderWidth)
        .setTo(component.width)
        .modify(_.components.at(componentId).params.renderHeight)
        .setTo(component.height)
        .modify(_.components.at(componentId).params.renderScale)
        .setTo(component.scale)
        .modify(_.components.at(componentId).params.renderRotation)
        .setTo(theta)
    }
  }

  override def updateComponentHitbox(creature: Creature, component: AbilityComponent): AbilityComponent = {

    val theta = new Vector2(component.params.dirVector.x, component.params.dirVector.y).angleDeg()

    val attackShiftX = component.params.dirVector.normal.x * component.params.attackRange
    val attackShiftY = component.params.dirVector.normal.y * component.params.attackRange

    val attackRectX = attackShiftX + creature.params.posX
    val attackRectY = attackShiftY + creature.params.posY

    component
      .modify(_.params.abilityHitbox)
      .setTo(
        AbilityHitbox(
          x = attackRectX,
          y = attackRectY,
          width = component.width,
          height = component.height,
          rotationAngle = theta,
          scale = component.scale
        )
      )

  }

  override def updateRenderPos(creature: Creature, component: AbilityComponent): AbilityComponent = {
    val attackShiftX = component.params.dirVector.normal.x * component.params.attackRange
    val attackShiftY = component.params.dirVector.normal.y * component.params.attackRange

    val attackRectX = attackShiftX + creature.params.posX
    val attackRectY = attackShiftY + creature.params.posY

    component
      .modify(_.params.renderPos)
      .setTo(Vec2(x = attackRectX, y = attackRectY))
  }

  def copy(
    params: AbilityParams = params,
    components: Map[String, AbilityComponent] = components
  ): ThrustWeaponAbility =
    ThrustWeaponAbility(params, components)
}
