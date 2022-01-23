package com.easternsauce.model.creature.ability.attack

import com.badlogic.gdx.math.Vector2
import com.easternsauce.model.creature.Creature
import com.easternsauce.model.creature.ability._
import com.softwaremill.quicklens.ModifyPimp

case class RegularAttack(
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
    initSpeed = 30f,
    range = 0f
  )

//  override val numOfComponents = 12
//
//  override def init(): Ability = {
//    val components = (for (i <- 0 until numOfComponents)
//      yield (
//        i.toString,
//        AbilityComponent(
//          AbilityComponentParams(componentId = i.toString, angleDeviation = (i - 1) * 30)
//        ) // TEST MULTIPLE COMPONENTS
//      )).toMap
//
//    this
//      .modify(_.components)
//      .setTo(components)
//  }

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

  def copy(params: AbilityParams = params, components: Map[String, AbilityComponent] = components): RegularAttack =
    RegularAttack(params, components)
}
