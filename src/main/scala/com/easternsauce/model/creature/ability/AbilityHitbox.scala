package com.easternsauce.model.creature.ability

case class AbilityHitbox(
  x: Float,
  y: Float,
  width: Float,
  height: Float,
  rotationAngle: Float = 0f,
  scale: Float = 1.0f
)
