package com.easternsauce.model.creature.ability.magic

import com.easternsauce.model.creature.ability._
import com.easternsauce.model.creature.ability.attack.RegularAttack

case class BubbleAbility(
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
    componentType = ComponentType.RangedProjectile,
    scale = 1.7f,
    initSpeed = 10f
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

//  override def updateComponentHitbox(creature: Creature, component: AbilityComponent): AbilityComponent = {
//
//    val theta = new Vector2(component.params.dirVector.x, component.params.dirVector.y).angleDeg()
//
//    val attackShiftX = component.params.dirVector.normal.x * component.params.attackRange
//    val attackShiftY = component.params.dirVector.normal.y * component.params.attackRange
//
//    val attackRectX = attackShiftX + creature.params.posX
//    val attackRectY = attackShiftY + creature.params.posY
//
//    component
//      .modify(_.params.abilityHitbox)
//      .setTo(
//        AbilityHitbox(
//          x = attackRectX,
//          y = attackRectY,
//          width = component.width,
//          height = component.height,
//          rotationAngle = theta,
//          scale = scale
//        )
//      )
//
//  }

  def copy(params: AbilityParams = params, components: Map[String, AbilityComponent] = components): RegularAttack =
    attack.RegularAttack(params, components)
}
