package com.easternsauce.model.creature.ability

import com.easternsauce.model.creature.ability.AbilityState.AbilityState
import com.easternsauce.model.util.SimpleTimer
import com.easternsauce.util.Vector2Wrapper

case class AbilityComponentParams(
  componentId: String,
  state: AbilityState = AbilityState.Inactive,
  activeTimer: SimpleTimer = SimpleTimer(),
  channelTimer: SimpleTimer = SimpleTimer(),
  abilityChannelAnimationTimer: SimpleTimer = SimpleTimer(),
  abilityActiveAnimationTimer: SimpleTimer = SimpleTimer(),
  dirVector: Vector2Wrapper = Vector2Wrapper(0, 0),
  abilityHitbox: AbilityHitbox = AbilityHitbox(0, 0, 1, 1, 0, 1),
  attackRange: Float = 1.8f
)
