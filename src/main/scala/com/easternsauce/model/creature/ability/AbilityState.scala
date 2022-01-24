package com.easternsauce.model.creature.ability

object AbilityState extends Enumeration {
  type AbilityState = Value
  val DelayedStart, Channel, Active, Inactive = Value
}
