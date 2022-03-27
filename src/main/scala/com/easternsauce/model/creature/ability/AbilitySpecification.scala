package com.easternsauce.model.creature.ability

import com.easternsauce.model.creature.ability.ComponentType.ComponentType

case class AbilitySpecification(
  textureWidth: Int,
  textureHeight: Int,
  totalActiveTime: Float,
  totalChannelTime: Float,
  channelSpriteType: String,
  activeSpriteType: String,
  channelFrameCount: Int,
  activeFrameCount: Int,
  channelFrameDuration: Float,
  activeFrameDuration: Float,
  componentType: ComponentType,
  scale: Float = 1.0f,
  initSpeed: Float = 0f,
  activeAnimationLooping: Boolean = false,
  channelAnimationLooping: Boolean = false
)
