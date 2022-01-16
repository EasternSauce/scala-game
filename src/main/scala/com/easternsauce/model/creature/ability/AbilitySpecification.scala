package com.easternsauce.model.creature.ability

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
  activeFrameDuration: Float
)
