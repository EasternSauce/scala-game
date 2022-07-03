package com.easternsauce.model.creature.ability.magic

import com.easternsauce.model.creature.ability.{Ability, AbilityComponent, AbilityParams, AbilitySpecification}

case class DashAbility(
  override val params: AbilityParams = AbilityParams(id = "dash"),
  override val components: Map[String, AbilityComponent] = Map()
) extends Ability(params = params, components = components) {
  override val specification: AbilitySpecification = ???

  override def copy(params: AbilityParams, components: Map[String, AbilityComponent]): Ability = ???

//  override onStart
}
