package com.easternsauce.model.creature.ability.sword

import com.badlogic.gdx.math.Vector2
import com.easternsauce.model.GameState
import com.easternsauce.model.creature.Creature
import com.easternsauce.model.creature.ability._
import com.easternsauce.util.Vector2Wrapper
import com.softwaremill.quicklens._

case class SwingWeaponAbility(
  override val params: AbilityParams = AbilityParams(),
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

  override def onStart(gameState: GameState, creatureId: String, abilityId: String): Ability = {
    val creature = gameState.creatures(creatureId)

    components.keys
      .foldLeft(this)((ability, componentId) => {
        val component = components(componentId)
        val dirVector = Vector2Wrapper(creature.params.dirVector.x, creature.params.dirVector.y)
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
          .setTo(Vector2Wrapper(x = creature.params.posX, y = creature.params.posY))
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
      .setTo(Vector2Wrapper(x = attackRectX, y = attackRectY))
  }

  def copy(params: AbilityParams = params, components: Map[String, AbilityComponent] = components): SwingWeaponAbility =
    SwingWeaponAbility(params, components)
}
