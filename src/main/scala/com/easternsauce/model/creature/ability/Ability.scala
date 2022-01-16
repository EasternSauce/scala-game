package com.easternsauce.model.creature.ability

import com.easternsauce.model.creature.Creature
import com.easternsauce.util.Vector2Wrapper
import com.softwaremill.quicklens._

abstract class Ability(val params: AbilityParams, val components: Map[String, AbilityComponent]) {

  val numOfComponents = 1

  def init(): Ability = {
    val components = (for (i <- 0 until numOfComponents)
      yield (i.toString, AbilityComponent(AbilityComponentParams(componentId = i.toString)))).toMap

    this
      .modify(_.components)
      .setTo(components)
  }

  def scale: Float = {
    //if (creature.isWeaponEquipped) creature.currentWeapon.template.attackScale.get
    //else
    1.4f
  }

  def stop(): Ability = {
    this // TODO?
  }

  def setNotOnCooldown(): Ability = this.modify(_.params.onCooldown).setTo(false)

  def setDirVector(dirVector: Vector2Wrapper): Ability =
    components.keys.foldLeft(this)((ability, componentId) => ability.setComponentDirVector(componentId, dirVector))

  def setComponentDirVector(componentId: String, dirVector: Vector2Wrapper): Ability = {

    this.modify(_.components.at(componentId).params.dirVector).setTo(dirVector)
  }

  def updateHitbox(creature: Creature): Ability = {
    components.keys.foldLeft(this)((ability, componentId) => ability.updateComponentHitbox(componentId, creature))
  }

  def updateComponentHitbox(componentId: String, creature: Creature): Ability = this

  def restartComponentActiveTimers(componentId: String): Ability = {
    this
      .modify(_.components.at(componentId).params.abilityActiveAnimationTimer)
      .using(_.restart())
      .modify(_.components.at(componentId).params.activeTimer)
      .using(_.restart())
  }

  def restartActiveTimers(): Ability = {
    components.keys.foldLeft(this)((ability, componentId) => ability.restartComponentActiveTimers(componentId))
  }

  def updateTimers(delta: Float): Ability = {
    components.keys.foldLeft(this)((ability, componentId) => ability.updateComponentTimers(componentId, delta))
  }

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

  def copy(params: AbilityParams = params, components: Map[String, AbilityComponent] = components): Ability

}
