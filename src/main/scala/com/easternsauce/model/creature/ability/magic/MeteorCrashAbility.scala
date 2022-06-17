package com.easternsauce.model.creature.ability.magic

import com.easternsauce.model.creature.Creature
import com.easternsauce.model.creature.ability._
import com.easternsauce.util.Vec2
import com.softwaremill.quicklens._

case class MeteorCrashAbility(
  override val params: AbilityParams = AbilityParams(id = "meteorCrash"),
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
    scale = 2f
  )

  override val abilityActiveSoundId: Option[String] = Some("explosion")

  override val numOfComponents: Int = 18 // divisible by 3
  val delayBetween = 0.3f

  override def init(): Ability = {

    val meteors1 = for (i <- 0 until numOfComponents / 3) yield "1_" + i.toString
    val meteors2 = for (i <- 0 until numOfComponents / 3) yield "2_" + i.toString
    val meteors3 = for (i <- 0 until numOfComponents / 3) yield "3_" + i.toString

    val components = (for (componentId <- meteors1 ++ meteors2 ++ meteors3)
      yield (componentId, AbilityComponent(specification, ComponentParams(componentId = componentId)))).toMap

    this
      .modify(_.components)
      .setTo(components)

  }

  override def onStart(creature: Creature): Ability = {

    val facingVector: Vec2 = creature.params.actionDirVector

    val meteors1 = for (i <- 0 until numOfComponents / 3) yield ("1_" + i.toString, i, 0)
    val meteors2 = for (i <- 0 until numOfComponents / 3) yield ("2_" + i.toString, i, 50)
    val meteors3 = for (i <- 0 until numOfComponents / 3) yield ("3_" + i.toString, i, -50)

    (meteors1 ++ meteors2 ++ meteors3).foldLeft(this)((ability, meteor) => {
      val (componentId, i, angle) = meteor

      val component = components(componentId)
      val vector: Vec2 = facingVector.rotate(angle.toFloat)

      val x = creature.params.posX + (3.125f * (i + 1)) * vector.x
      val y = creature.params.posY + (3.125f * (i + 1)) * vector.y

      val scale = (i + 1) / 5f * component.scale

      ability
        .modify(_.components.at(componentId).params.range)
        .setTo(1.5625f + 0.09375f * i * i)
        .modify(_.components.at(componentId).params.speed)
        .setTo(2.5f)
        .modify(_.components.at(componentId).params.delay)
        .setTo(0.1f * i)
        .modify(_.components.at(componentId).params.abilityHitbox)
        .setTo(AbilityHitbox(x = x, y = y, width = component.width, height = component.height, scale = scale))
        .modify(_.components.at(componentId).params.renderPos)
        .setTo(Vec2(x = x, y = y))
        .modify(_.components.at(componentId).params.renderWidth)
        .setTo(component.width)
        .modify(_.components.at(componentId).params.renderHeight)
        .setTo(component.height)
        .modify(_.components.at(componentId).params.renderScale)
        .setTo(scale)
    })

  }

  def copy(params: AbilityParams = params, components: Map[String, AbilityComponent] = components): MeteorCrashAbility =
    MeteorCrashAbility(params, components)
}
