package com.easternsauce.model.creature

import com.easternsauce.util.Direction
import com.easternsauce.util.Direction.Direction

case class Ghost(override val params: CreatureParams) extends Creature {
  override val spriteType: String = "ghost"
  override val textureWidth: Int = 32
  override val textureHeight: Int = 32
  override val width: Float = 2.85f
  override val height: Float = 2.85f
  override val frameDuration: Float = 0.25f
  override val frameCount: Int = 3
  override val neutralStanceFrame: Int = 1
  override val dirMap: Map[Direction, Int] =
    Map(Direction.Up -> 3, Direction.Down -> 0, Direction.Left -> 1, Direction.Right -> 2)
  override val baseLife: Float = 300f

  override def update(delta: Float): Ghost = {
    super.update(delta).asInstanceOf[Ghost]
  }

  override def copy(params: CreatureParams): Ghost = {
    Ghost(params)
  }
}
