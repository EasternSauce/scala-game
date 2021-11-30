package model.creature

import util.Direction

case class Skeleton(override val params: Creature.Params) extends Creature {

  override val spriteType: String = "skeleton"
  override val textureWidth: Int = 64
  override val textureHeight: Int = 64
  override val width: Float = 2
  override val height: Float = 2
  override val frameDuration: Float = 0.05f
  override val frameCount: Int = 9
  override val neutralStanceFrame: Int = 1
  override val dirMap: Map[Direction.Value, Int] =
    Map(Direction.Up -> 0, Direction.Down -> 2, Direction.Left -> 1, Direction.Right -> 3)

  override def update(delta: Float): Skeleton = {
    super.update(delta).asInstanceOf[Skeleton]
  }

  override def copy(params: Creature.Params): Skeleton = {
    Skeleton(params)
  }
}
