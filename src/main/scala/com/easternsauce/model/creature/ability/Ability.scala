package com.easternsauce.model.creature.ability

import com.easternsauce.model.creature.Creature
import com.softwaremill.quicklens._

abstract class Ability(val params: AbilityParams, val components: Map[String, AbilityComponent]) {

  val specification: AbilitySpecification

  val numOfComponents = 1
  val cooldownTime: Float = 0.8f

  def init(): Ability = {
    val components = (for (i <- 0 until numOfComponents)
      yield (i.toString, AbilityComponent(specification, AbilityComponentParams(componentId = i.toString)))).toMap

    this
      .modify(_.components)
      .setTo(components)
  }

  def scale: Float = {
    //if (creature.isWeaponEquipped) creature.currentWeapon.template.attackScale.get
    //else
    1.4f
  }

  def setNotOnCooldown(): Ability = this.modify(_.params.onCooldown).setTo(false)

  def updateHitbox(creature: Creature): Ability = {
    components.keys.foldLeft(this)((ability, componentId) => ability.updateComponentHitbox(componentId, creature))
  }

  def updateComponentHitbox(componentId: String, creature: Creature): Ability = this

  def updateComponentTimers(componentId: String, delta: Float): Ability = {
    this
      .modify(_.components.at(componentId).params.activeTimer)
      .using(_.update(delta))
      .modify(_.components.at(componentId).params.channelTimer)
      .using(_.update(delta))
      .modify(_.components.at(componentId).params.abilityChannelAnimationTimer)
      .using(_.update(delta))
      .modify(_.components.at(componentId).params.abilityActiveAnimationTimer)
      .using(_.update(delta))
  }

  def componentsActive: Boolean = components.values.exists(component => component.params.state != AbilityState.Inactive)

  def copy(params: AbilityParams = params, components: Map[String, AbilityComponent] = components): Ability

}
