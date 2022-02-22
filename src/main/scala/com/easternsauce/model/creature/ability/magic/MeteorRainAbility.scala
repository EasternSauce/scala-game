package com.easternsauce.model.creature.ability.magic

import com.easternsauce.model.GameState
import com.easternsauce.model.creature.ability._
import com.easternsauce.system.Random
import com.easternsauce.util.Vector2Wrapper
import com.softwaremill.quicklens._

case class MeteorRainAbility(
  override val params: AbilityParams = AbilityParams(),
  override val components: Map[String, AbilityComponent] = Map()
) extends Ability(params = params, components = components) {

  override val specification: AbilitySpecification = AbilitySpecification(
    textureWidth = 64,
    textureHeight = 64,
    totalActiveTime = 0.5f,
    totalChannelTime = 0.5f,
    channelSpriteType = "explosion_windup",
    activeSpriteType = "explosion",
    channelFrameCount = 7,
    activeFrameCount = 14,
    channelFrameDuration = 0.071428f,
    activeFrameDuration = 0.035714f,
    componentType = ComponentType.RainingProjectile,
    scale = 1.4f,
    range = 8f
  )

  override val numOfComponents: Int = 18
  val delayBetween = 0.3f

  override def init(): Ability = {

    val components = (for (i <- 0 until numOfComponents)
      yield (
        i.toString,
        AbilityComponent(specification, ComponentParams(componentId = i.toString, delay = i * delayBetween))
      )).toMap

    this
      .modify(_.components)
      .setTo(components)
  }

  override def onStart(gameState: GameState, creatureId: String, abilityId: String): Ability = {
    val creature = gameState.creatures(creatureId)

    components.keys
      .foldLeft(this)((ability, componentId) => {
        val component = components(componentId)
        val x = creature.params.posX + Random.between(-specification.range, specification.range)
        val y = creature.params.posY + Random.between(-specification.range, specification.range)

        ability
          .modify(_.components.at(componentId).params.abilityHitbox)
          .setTo(
            AbilityHitbox(
              x = x,
              y = y,
              width = component.textureWidth,
              height = component.textureHeight,
              scale = component.scale
            )
          )
          .modify(_.components.at(componentId).params.renderPos)
          .setTo(Vector2Wrapper(x = x, y = y))
          .modify(_.components.at(componentId).params.renderWidth)
          .setTo(component.textureWidth)
          .modify(_.components.at(componentId).params.renderHeight)
          .setTo(component.textureHeight)
          .modify(_.components.at(componentId).params.renderScale)
          .setTo(component.scale)

      })
  }

  def copy(params: AbilityParams = params, components: Map[String, AbilityComponent] = components): MeteorRainAbility =
    MeteorRainAbility(params, components)
}
