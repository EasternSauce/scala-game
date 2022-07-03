package com.easternsauce.model.creature.ability.magic

import com.easternsauce.model.GameState
import com.easternsauce.model.creature.ability._
import com.easternsauce.util.Vec2
import com.softwaremill.quicklens._

case class BubbleAbility(
  override val params: AbilityParams = AbilityParams(id = "bubble"),
  override val components: Map[String, AbilityComponent] = Map()
) extends Ability(params = params, components = components) {

  override val specification: Option[AbilitySpecification] = Some(
    AbilitySpecification(
      textureWidth = 64,
      textureHeight = 64,
      totalActiveTime = 1.5f,
      totalChannelTime = 0.5f,
      channelSpriteType = "bubble",
      activeSpriteType = "bubble",
      channelFrameCount = 2,
      activeFrameCount = 2,
      channelFrameDuration = 0.1f,
      activeFrameDuration = 0.3f,
      componentType = ComponentType.RangedProjectile,
      scale = 1.7f,
      initSpeed = 12f,
      activeAnimationLooping = true
    )
  )

  //  override val abilityActiveSoundId: Option[String] = Some("explosion") TODO

  override val isDestroyOnCollision: Boolean = true

  override val numOfComponents: Int = 3
  val delayBetween = 0.8f

  override def init(): Ability = {

    val components = (for (i <- 0 until numOfComponents)
      yield (
        i.toString,
        AbilityComponent(
          specification.get, {
            ComponentParams(componentId = i.toString, delay = i * delayBetween)
          }
        )
      )).toMap

    this
      .modify(_.components)
      .setTo(components)
  }

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

  def copy(params: AbilityParams = params, components: Map[String, AbilityComponent] = components): BubbleAbility =
    BubbleAbility(params, components)

}
