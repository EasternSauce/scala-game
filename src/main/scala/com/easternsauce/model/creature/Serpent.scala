package com.easternsauce.model.creature

import com.easternsauce.util.Direction
import com.easternsauce.util.Direction.Direction

case class Serpent(override val params: CreatureParams) extends Enemy(params = params) {

  override val spriteType: String = "serpent"
  override val textureWidth: Int = 48
  override val textureHeight: Int = 56
  override val width: Float = 3.85f
  override val height: Float = 3.85f
  override val frameDuration: Float = 0.15f
  override val frameCount: Int = 3
  override val neutralStanceFrame: Int = 0
  override val dirMap: Map[Direction, Int] =
    Map(Direction.Up -> 0, Direction.Down -> 2, Direction.Left -> 1, Direction.Right -> 3)
  override val baseLife: Float = 160f

  override val dropTable = Map(
    "ringmailGreaves" -> 0.1f,
    "leatherArmor" -> 0.05f,
    "hideGloves" -> 0.1f,
    "leatherHelmet" -> 0.1f,
    "woodenSword" -> 0.1f,
    "healingPowder" -> 0.5f
  )

  override def update(delta: Float): Serpent = {
    super.update(delta).asInstanceOf[Serpent]
  }

  override def copy(params: CreatureParams): Serpent = {
    Serpent(params)
  }
}
