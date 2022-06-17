package com.easternsauce.model.creature

import com.easternsauce.util.Direction
import com.easternsauce.util.Direction.Direction

case class Player(override val params: CreatureParams) extends Creature {

  override val spriteType: String = "male1"
  override val textureWidth: Int = 32
  override val textureHeight: Int = 32
  override val width: Float = 2
  override val height: Float = 2
  override val frameDuration: Float = 0.1f
  override val frameCount: Int = 3
  override val neutralStanceFrame: Int = 1
  override val dirMap: Map[Direction, Int] =
    Map(Direction.Up -> 3, Direction.Down -> 0, Direction.Left -> 1, Direction.Right -> 2)
  override val baseLife: Float = 100f

  override val isPlayer = true

  override val speed: Float = 25f

  override val onGettingHitSoundId: Option[String] = Some("pain")

  override def update(delta: Float): Player = {
    super.update(delta).asInstanceOf[Player]
  }

  override def copy(params: CreatureParams): Player = {
    Player(params)
  }

}
