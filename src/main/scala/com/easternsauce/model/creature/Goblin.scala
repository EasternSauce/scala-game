package com.easternsauce.model.creature

import com.easternsauce.util.Direction
import com.easternsauce.util.Direction.Direction

case class Goblin(override val params: CreatureParams) extends Creature {
  override val spriteType: String = "goblin"
  override val textureWidth: Int = 32
  override val textureHeight: Int = 32
  override val width: Float = 2.85f
  override val height: Float = 2.85f
  override val frameDuration: Float = 0.25f
  override val frameCount: Int = 3
  override val neutralStanceFrame: Int = 1
  override val dirMap: Map[Direction, Int] =
    Map(Direction.Up -> 3, Direction.Down -> 0, Direction.Left -> 1, Direction.Right -> 2)
  override val baseLife: Float = 190f

  override val dropTable = Map(
    "ironSword" -> 0.03f,
    "poisonDagger" -> 0.07f,
    "healingPowder" -> 0.3f,
    "steelArmor" -> 0.03f,
    "steelGreaves" -> 0.05f,
    "steelGloves" -> 0.05f,
    "steelHelmet" -> 0.05f
  )

  override def update(delta: Float): Goblin = {
    super.update(delta).asInstanceOf[Goblin]
  }

  override def copy(params: CreatureParams): Goblin = {
    Goblin(params)
  }
}
