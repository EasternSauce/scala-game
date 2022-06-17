package com.easternsauce.model.creature.ability.magic

import com.easternsauce.model.creature.Creature
import com.easternsauce.model.creature.ability._
import com.easternsauce.system.Random
import com.easternsauce.util.Vec2
import com.softwaremill.quicklens._

case class FistSlamAbility(
  override val params: AbilityParams = AbilityParams(id = "fistSlam"),
  override val components: Map[String, AbilityComponent] = Map()
) extends Ability(params = params, components = components) {

  override val specification: AbilitySpecification = AbilitySpecification(
    textureWidth = 40,
    textureHeight = 80,
    totalActiveTime = 0.5f,
    totalChannelTime = 0.2f,
    channelSpriteType = "fist_slam_windup",
    activeSpriteType = "fist_slam",
    channelFrameCount = 5,
    activeFrameCount = 5,
    channelFrameDuration = 0.04f,
    activeFrameDuration = 0.05f,
    componentType = ComponentType.RainingProjectile,
    scale = 1.5f
  )

  override val abilityActiveSoundId: Option[String] = Some("glass-break")

  override val numOfComponents: Int = 18
  val delayBetween = 0.3f

  override def init(): Ability = {

    val components = (for (i <- 0 until numOfComponents)
      yield (
        i.toString,
        AbilityComponent(specification, ComponentParams(componentId = i.toString, delay = i * delayBetween, range = 4f))
      )).toMap

    this
      .modify(_.components)
      .setTo(components)
  }

  override def onStart(creature: Creature): Ability = {

    components.keys
      .foldLeft(this)((ability, componentId) => {
        val component = components(componentId)
        val x = creature.params.posX + Random.between(-component.params.range, component.params.range)
        val y = creature.params.posY + Random.between(-component.params.range, component.params.range)

        ability
          .modify(_.components.at(componentId).params.abilityHitbox)
          .setTo(
            AbilityHitbox(
              x = x,
              y = y - component.height / 4 * component.scale, // shift hitbox downwards
              width = component.width,
              height = component.height / 2,
              scale = component.scale
            )
          )
          .modify(_.components.at(componentId).params.renderPos)
          .setTo(Vec2(x = x, y = y))
          .modify(_.components.at(componentId).params.renderWidth)
          .setTo(component.width)
          .modify(_.components.at(componentId).params.renderHeight)
          .setTo(component.height)
          .modify(_.components.at(componentId).params.renderScale)
          .setTo(component.scale)

      })
  }

  def copy(params: AbilityParams = params, components: Map[String, AbilityComponent] = components): FistSlamAbility =
    FistSlamAbility(params, components)
}
