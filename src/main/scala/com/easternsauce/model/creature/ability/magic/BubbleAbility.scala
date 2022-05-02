package com.easternsauce.model.creature.ability.magic

import com.easternsauce.model.creature.Creature
import com.easternsauce.model.creature.ability._
import com.easternsauce.util.Vector2Wrapper
import com.softwaremill.quicklens._

case class BubbleAbility(
  override val params: AbilityParams = AbilityParams(id = "bubble"),
  override val components: Map[String, AbilityComponent] = Map()
) extends Ability(params = params, components = components) {

  override val specification: AbilitySpecification = AbilitySpecification(
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

  override val numOfComponents: Int = 3
  val delayBetween = 0.8f

  override def init(): Ability = {

    val components = (for (i <- 0 until numOfComponents)
      yield (
        i.toString,
        AbilityComponent(
          specification, {
            ComponentParams(componentId = i.toString, delay = i * delayBetween)
          }
        )
      )).toMap

    this
      .modify(_.components)
      .setTo(components)
  }

  override def onStart(creature: Creature): Ability = {
    components.keys
      .foldLeft(this)((ability, componentId) => {
        val component = components(componentId)
        val dirVector = Vector2Wrapper(creature.params.actionDirVector.x, creature.params.actionDirVector.y)
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

  def copy(params: AbilityParams = params, components: Map[String, AbilityComponent] = components): BubbleAbility =
    BubbleAbility(params, components)

}
