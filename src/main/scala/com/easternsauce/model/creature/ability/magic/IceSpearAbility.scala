package com.easternsauce.model.creature.ability.magic

import com.easternsauce.model.creature.Creature
import com.easternsauce.model.creature.ability._
import com.easternsauce.util.Vector2Wrapper
import com.softwaremill.quicklens._

case class IceSpearAbility(
  override val params: AbilityParams = AbilityParams(id = "iceSpear"),
  override val components: Map[String, AbilityComponent] = Map()
) extends Ability(params = params, components = components) {

  override val specification: AbilitySpecification = AbilitySpecification(
    textureWidth = 152,
    textureHeight = 72,
    totalActiveTime = 3f,
    totalChannelTime = 0.01f,
    channelSpriteType = "ice_shard",
    activeSpriteType = "ice_shard",
    channelFrameCount = 1,
    activeFrameCount = 1,
    channelFrameDuration = 0.1f,
    activeFrameDuration = 0.3f,
    componentType = ComponentType.RangedProjectile,
    scale = 0.8f,
    initSpeed = 20f
  )

//  override val abilityActiveSoundId: Option[String] = Some("explosion") TODO

  override val numOfComponents: Int = 5
  val delayBetween = 0.1f

  override def init(): Ability = {

    val components = (for (i <- 0 until numOfComponents)
      yield (
        i.toString,
        AbilityComponent(
          specification, {
            ComponentParams(componentId = i.toString, angleDeviation = 10f * (i - 2), delay = i * delayBetween)
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

  def copy(params: AbilityParams = params, components: Map[String, AbilityComponent] = components): IceSpearAbility =
    IceSpearAbility(params, components)
}
