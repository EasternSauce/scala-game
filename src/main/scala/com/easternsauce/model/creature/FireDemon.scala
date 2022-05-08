package com.easternsauce.model.creature

import com.easternsauce.util.Direction
import com.easternsauce.util.Direction.Direction

case class FireDemon(override val params: CreatureParams) extends Enemy(params = params) {
  override val spriteType: String = "taurus"
  override val textureWidth: Int = 80
  override val textureHeight: Int = 80
  override val width: Float = 7.5f
  override val height: Float = 7.5f
  override val frameDuration: Float = 0.15f
  override val frameCount: Int = 4
  override val neutralStanceFrame: Int = 0
  override val dirMap: Map[Direction, Int] =
    Map(Direction.Left -> 1, Direction.Right -> 2, Direction.Up -> 3, Direction.Down -> 0)
  override val baseLife: Float = 5500f

  override val dropTable =
    Map("ironSword" -> 0.3f, "poisonDagger" -> 0.3f, "steelArmor" -> 0.8f, "steelHelmet" -> 0.5f, "thiefRing" -> 1.0f)


  override def update(delta: Float): FireDemon = {
    super.update(delta).asInstanceOf[FireDemon]
  }

  override def copy(params: CreatureParams): FireDemon = {
    FireDemon(params)
  }
}
