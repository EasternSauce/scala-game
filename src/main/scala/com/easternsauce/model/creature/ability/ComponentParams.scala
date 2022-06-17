package com.easternsauce.model.creature.ability

import com.easternsauce.model.creature.ability.AbilityState.AbilityState
import com.easternsauce.model.util.SimpleTimer
import com.easternsauce.util.Vec2

case class ComponentParams(
  componentId: String,
  state: AbilityState = AbilityState.Inactive,
  activeTimer: SimpleTimer = SimpleTimer(),
  channelTimer: SimpleTimer = SimpleTimer(),
  abilityChannelAnimationTimer: SimpleTimer = SimpleTimer(),
  abilityActiveAnimationTimer: SimpleTimer = SimpleTimer(),
  dirVector: Vec2 = Vec2(0, 0),
  renderPos: Vec2 = Vec2(0, 0),
  renderWidth: Float = 0f,
  renderHeight: Float = 0f,
  renderScale: Float = 1.0f,
  renderRotation: Float = 0.0f,
  abilityHitbox: AbilityHitbox = AbilityHitbox(0, 0, 1, 1, 0, 1),
  attackRange: Float = 1.8f,
  angleDeviation: Float = 0f,
  delay: Float = 0f,
  speed: Float = 1.0f, // cannot be zero!
  range: Float = 0f
)
