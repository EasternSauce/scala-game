package com.easternsauce.model.creature

import com.easternsauce.util.Direction
import com.easternsauce.util.Direction.Direction

object Skeleton {
  def apply(params: CreatureParams): Creature = new Creature(params) {
    override val spriteType: String = "skeleton"
    override val textureWidth: Int = 64
    override val textureHeight: Int = 64
    override val width: Float = 2
    override val height: Float = 2
    override val frameDuration: Float = 0.05f
    override val frameCount: Int = 9
    override val neutralStanceFrame: Int = 1
    override val dirMap: Map[Direction, Int] =
      Map(Direction.Up -> 0, Direction.Down -> 2, Direction.Left -> 1, Direction.Right -> 3)

    override def update(delta: Float): Creature = {
      super.update(delta)
    }
  }
}