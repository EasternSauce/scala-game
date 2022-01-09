package com.easternsauce.model.creature

import com.easternsauce.util.Direction
import com.easternsauce.util.Direction.Direction

case class Wolf(override val params: CreatureParams) extends Creature {
  override val spriteType: String = "wolf2"
  override val textureWidth: Int = 32
  override val textureHeight: Int = 34
  override val width: Float = 2.85f
  override val height: Float = 2.85f
  override val frameDuration: Float = 0.1f
  override val frameCount: Int = 6
  override val neutralStanceFrame: Int = 1
  override val dirMap: Map[Direction, Int] =
    Map(Direction.Up -> 3, Direction.Down -> 0, Direction.Left -> 1, Direction.Right -> 2)
  override val baseLife: Float = 150f

  override def update(delta: Float): Wolf = {
    super.update(delta).asInstanceOf[Wolf]
  }

  override def copy(params: CreatureParams): Wolf = {
    Wolf(params)
  }
}
