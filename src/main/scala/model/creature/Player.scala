package model.creature

import util.Direction

case class Player(override val params: Creature.Params) extends Creature {

  override val spriteType: String = "male1"
  override val textureWidth: Int = 32
  override val textureHeight: Int = 32
  override val width: Float = 2
  override val height: Float = 2
  override val frameDuration: Float = 0.1f
  override val frameCount: Int = 3
  override val neutralStanceFrame: Int = 1
  override val dirMap: Map[Direction.Value, Int] =
    Map(Direction.Up -> 3, Direction.Down -> 0, Direction.Left -> 1, Direction.Right -> 2)

  override val isPlayer = true

  override def update(delta: Float): Player = {
    super.update(delta).asInstanceOf[Player]
  }

  override def copy(params: Creature.Params): Player = {
    Player(params)
  }

}
