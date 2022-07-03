package com.easternsauce.model.creature.ability.sword

import com.badlogic.gdx.math.Vector2
import com.easternsauce.model.GameState
import com.easternsauce.model.creature.Creature
import com.easternsauce.model.creature.ability._
import com.easternsauce.util.Vec2
import com.softwaremill.quicklens._

case class SwingWeaponAbility(
  override val params: AbilityParams = AbilityParams(id = "swingWeapon"),
  override val components: Map[String, AbilityComponent] = Map()
) extends Ability(params = params, components = components) {

  override val specification: AbilitySpecification = AbilitySpecification(
    textureWidth = 40,
    textureHeight = 40,
    totalActiveTime = 0.3f,
    totalChannelTime = 0.3f,
    channelSpriteType = "slash_windup",
    activeSpriteType = "slash",
    channelFrameCount = 6,
    activeFrameCount = 6,
    channelFrameDuration = 0.05f,
    activeFrameDuration = 0.05f,
    componentType = ComponentType.MeleeAttack,
    scale = 1.4f,
    initSpeed = 30f
  )

  override val abilityActiveSoundId: Option[String] = Some("swoosh")

  override val isWeaponAttack = true

  override def onStart(creatureId: String): GameState => GameState = { gameState =>
    gameState.modifyGameStateAbility(creatureId, abilityId) { ability =>
      val creature = gameState.creatures(creatureId)
      ability.components.keys
        .foldLeft(this)((ability, componentId) => {
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
        })
    }
  }

  override def updateComponentHitbox(creature: Creature, component: AbilityComponent): AbilityComponent = {
    val dirVector = component.params.dirVector match {
      case dirVector if dirVector.length <= 0 => Vec2(1, 0).normal
      case dirVector                          => dirVector
    }

    val theta = new Vector2(dirVector.x, dirVector.y).angleDeg()

    val attackShiftX = dirVector.normal.x * component.params.attackRange
    val attackShiftY = dirVector.normal.y * component.params.attackRange

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

    val dirVector = component.params.dirVector match {
      case dirVector if dirVector.length <= 0 => Vec2(1, 0).normal
      case dirVector                          => dirVector
    }

    val attackShiftX = dirVector.normal.x * component.params.attackRange
    val attackShiftY = dirVector.normal.y * component.params.attackRange

    val attackRectX = attackShiftX + creature.params.posX
    val attackRectY = attackShiftY + creature.params.posY

    component
      .modify(_.params.renderPos)
      .setTo(Vec2(x = attackRectX, y = attackRectY))
  }

  def copy(params: AbilityParams = params, components: Map[String, AbilityComponent] = components): SwingWeaponAbility =
    SwingWeaponAbility(params, components)
}
