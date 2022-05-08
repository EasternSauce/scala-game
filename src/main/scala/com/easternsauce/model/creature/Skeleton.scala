package com.easternsauce.model.creature

import com.easternsauce.util.Direction
import com.easternsauce.util.Direction.Direction

case class Skeleton(override val params: CreatureParams) extends Enemy(params = params) {

  override val spriteType: String = "skeleton"
  override val textureWidth: Int = 64
  override val textureHeight: Int = 64
  override val width: Float = 2
  override val height: Float = 2
  override val frameDuration: Float = 0.05f
  override val frameCount: Int = 9
  override val neutralStanceFrame: Int = 0
  override val dirMap: Map[Direction, Int] =
    Map(Direction.Up -> 0, Direction.Down -> 2, Direction.Left -> 1, Direction.Right -> 3)
  override val baseLife: Float = 200f

  override val dropTable = Map(
    "ringmailGreaves" -> 0.1f,
    "leatherArmor" -> 0.05f,
    "hideGloves" -> 0.1f,
    "leatherHelmet" -> 0.1f,
    "woodenSword" -> 0.1f,
    "healingPowder" -> 0.5f
  )

  override def update(delta: Float): Skeleton = {
    super.update(delta).asInstanceOf[Skeleton]
  }

  override def copy(params: CreatureParams): Skeleton = {
    Skeleton(params)
  }
}
