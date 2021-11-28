package data

import util.Direction

case class SpriteTextureData(
  spriteType: String,
  boundsWidth: Int,
  boundsHeight: Int,
  textureWidth: Int,
  textureHeight: Int,
  dirMap: Map[Direction.Value, Int]
)
