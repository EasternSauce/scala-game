package com.easternsauce.model.creature.ability.magic

import com.easternsauce.model.GameState
import com.easternsauce.model.creature.ability._
import com.easternsauce.system.Random
import com.easternsauce.util.Vec2
import com.softwaremill.quicklens._

case class MeteorRainAbility(
  override val params: AbilityParams = AbilityParams(id = "meteorRain"),
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
    scale = 1.4f
  )

  override val abilityActiveSoundId: Option[String] = Some("explosion")

  override val numOfComponents: Int = 18
  val delayBetween = 0.3f

  override def init(): Ability = {

    val components = (for (i <- 0 until numOfComponents)
      yield (
        i.toString,
        AbilityComponent(specification, ComponentParams(componentId = i.toString, delay = i * delayBetween, range = 8f))
      )).toMap

    this
      .modify(_.components)
      .setTo(components)
  }

  override def onStart(creatureId: String): GameState => GameState = { gameState =>
    val creature = gameState.creatures(creatureId)
    gameState.modifyEachAbilityComponent(creatureId, abilityId) { (ability, componentId) =>
      val component = components(componentId)
      val x = creature.params.posX + Random.between(-component.params.range, component.params.range)
      val y = creature.params.posY + Random.between(-component.params.range, component.params.range)

      ability
        .modify(_.components.at(componentId).params.abilityHitbox)
        .setTo(AbilityHitbox(x = x, y = y, width = component.width, height = component.height, scale = component.scale))
        .modify(_.components.at(componentId).params.renderPos)
        .setTo(Vec2(x = x, y = y))
        .modify(_.components.at(componentId).params.renderWidth)
        .setTo(component.width)
        .modify(_.components.at(componentId).params.renderHeight)
        .setTo(component.height)
        .modify(_.components.at(componentId).params.renderScale)
        .setTo(component.scale)

    }
  }

  def copy(params: AbilityParams = params, components: Map[String, AbilityComponent] = components): MeteorRainAbility =
    MeteorRainAbility(params, components)
}
