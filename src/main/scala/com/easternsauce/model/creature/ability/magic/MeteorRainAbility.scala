package com.easternsauce.model.creature.ability.magic

import com.badlogic.gdx.math.Vector2
import com.easternsauce.model.GameState
import com.easternsauce.model.creature.ability._
import com.easternsauce.system.Random
import com.softwaremill.quicklens._

case class MeteorRainAbility(
  override val params: AbilityParams = AbilityParams(),
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
    componentType = ComponentType.RainingProjectile,
    scale = 1.3f,
    initSpeed = 10f,
    range = 3f
  )

  override val numOfComponents: Int = 10

  override def init(): Ability = {

    val components = (for (i <- 0 until numOfComponents)
      yield (i.toString, AbilityComponent(specification, ComponentParams(componentId = i.toString)))).toMap

    this
      .modify(_.components)
      .setTo(components)
  }

  override def onStart(gameState: GameState, creatureId: String, abilityId: String): Ability = {
    val creature = gameState.creatures(creatureId)

    components.keys
      .foldLeft(this)((ability, componentId) => {
        val component = components(componentId)
        val theta = new Vector2(component.params.dirVector.x, component.params.dirVector.y).angleDeg()
        val x = creature.params.posX + Random.between(-specification.range, specification.range)
        val y = creature.params.posY + Random.between(-specification.range, specification.range)

        ability
          .modify(_.components.at(componentId).params.abilityHitbox)
          .setTo(
            AbilityHitbox(
              x = x,
              y = y,
              width = component.width,
              height = component.height,
              rotationAngle = theta,
              scale = component.scale
            )
          )

      })
  }

  def copy(params: AbilityParams = params, components: Map[String, AbilityComponent] = components): MeteorRainAbility =
    MeteorRainAbility(params, components)
}
