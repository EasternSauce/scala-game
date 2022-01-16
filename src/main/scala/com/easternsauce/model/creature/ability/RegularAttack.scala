package com.easternsauce.model.creature.ability

import com.badlogic.gdx.math.Vector2
import com.easternsauce.model.creature.Creature
import com.softwaremill.quicklens._

case class RegularAttack(
  override val params: AbilityParams = AbilityParams(),
  override val components: Map[String, AbilityComponent] = Map()
) extends Ability(params = params, components = components) {

  override val numOfComponents = 12

  override def init(): Ability = {
    val components = (for (i <- 0 until numOfComponents)
      yield (
        i.toString,
        AbilityComponent(
          AbilityComponentParams(componentId = i.toString, angleDeviation = (i - 1) * 30)
        ) // TEST MULTIPLE COMPONENTS
      )).toMap

    this
      .modify(_.components)
      .setTo(components)
  }

  override def updateComponentHitbox(componentId: String, creature: Creature): Ability = {

    val component = components(componentId)

    val theta = new Vector2(component.params.dirVector.x, component.params.dirVector.y).angleDeg()

    val attackShiftX = component.params.dirVector.normal.x * component.params.attackRange
    val attackShiftY = component.params.dirVector.normal.y * component.params.attackRange

    val attackRectX = attackShiftX + creature.params.posX
    val attackRectY = attackShiftY + creature.params.posY

    val updatedComponent = component
      .modify(_.params.abilityHitbox)
      .setTo(
        AbilityHitbox(
          x = attackRectX,
          y = attackRectY,
          width = component.width,
          height = component.height,
          rotationAngle = theta,
          scale = scale
        )
      )

    this.modify(_.components.at(componentId)).setTo(updatedComponent)
  }

  def copy(params: AbilityParams = params, components: Map[String, AbilityComponent] = components): RegularAttack =
    RegularAttack(params, components)
}
