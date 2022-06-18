package com.easternsauce.model.creature.ability

import com.easternsauce.model.creature.Creature
import com.easternsauce.util.Vec2
import com.softwaremill.quicklens._

abstract class Ability(val params: AbilityParams, val components: Map[String, AbilityComponent]) {

  val specification: AbilitySpecification

  val isWeaponAttack = false

  val numOfComponents = 1
  val cooldownTime: Float = 1f

  val abilityChannelSoundId: Option[String] = None
  val abilityActiveSoundId: Option[String] = Some("swoosh")

  def init(): Ability = {
    val components = (for (i <- 0 until numOfComponents)
      yield (i.toString, AbilityComponent(specification, ComponentParams(componentId = i.toString)))).toMap

    this
      .modify(_.components)
      .setTo(components)
  }

  def onStart(creature: Creature): Ability = this

//  def scale: Float = {
//    //if (creature.isWeaponEquipped) creature.currentWeapon.template.attackScale.get
//    //else
//    1.4f
//  }

  def onCooldown: Boolean = if (params.abilityTimer.isRunning) params.abilityTimer.time < cooldownTime else false

  def updateComponentHitbox(creature: Creature, component: AbilityComponent): AbilityComponent = component

  def updateRenderPos(creature: Creature, component: AbilityComponent): AbilityComponent = {
    component
      .modify(_.params.renderPos)
      .setTo(Vec2(x = component.params.abilityHitbox.x, y = component.params.abilityHitbox.y))
  }

  def updateComponentTimers(component: AbilityComponent, delta: Float): AbilityComponent = {
    component
      .modify(_.params.activeTimer)
      .using(_.update(delta))
      .modify(_.params.channelTimer)
      .using(_.update(delta))
      .modify(_.params.abilityChannelAnimationTimer)
      .using(_.update(delta))
      .modify(_.params.abilityActiveAnimationTimer)
      .using(_.update(delta))
  }

  def updateTimers(delta: Float): Ability = {
    this.modify(_.params.abilityTimer).using(_.update(delta))
  }

  def stop(): Ability = {
    this.modify(_.components.each).using(_.stop())
  }

  def componentsActive: Boolean = components.values.exists(component => component.params.state != AbilityState.Inactive)

  def copy(params: AbilityParams = params, components: Map[String, AbilityComponent] = components): Ability

}
