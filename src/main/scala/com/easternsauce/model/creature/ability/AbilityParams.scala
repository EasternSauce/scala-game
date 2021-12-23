package com.easternsauce.model.creature.ability

import com.badlogic.gdx.math.Vector2
import com.easternsauce.model.creature.ability.AbilityState.AbilityState
import com.easternsauce.model.util.SimpleTimer

case class AbilityParams(
  state: AbilityState = AbilityState.Inactive,
  onCooldown: Boolean = false,
  activeTimer: SimpleTimer = SimpleTimer(),
  channelTimer: SimpleTimer = SimpleTimer(),
  abilityChannelAnimationTimer: SimpleTimer = SimpleTimer(),
  abilityActiveAnimationTimer: SimpleTimer = SimpleTimer(),
  dirVector: Vector2 = new Vector2(0, 0),
  abilityHitbox: AbilityHitbox = AbilityHitbox(0, 0, 1, 1, 0, 1),
  attackRange: Option[Float] = Some(1.8f)
)
