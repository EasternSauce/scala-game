package com.easternsauce.model.creature

import com.easternsauce.model.creature.Creature.Params
import com.easternsauce.model.util.SimpleTimer
import com.easternsauce.util.Direction
import com.softwaremill.quicklens.ModifyPimp

abstract class Creature {
  val isPlayer = false

  val params: Params
  val spriteType: String
  val textureWidth: Int
  val textureHeight: Int
  val width: Float
  val height: Float
  val frameDuration: Float
  val frameCount: Int
  val neutralStanceFrame: Int
  val dirMap: Map[Direction.Value, Int]

  def update(delta: Float): Creature = {
    this.updateTimers(delta)
  }

  def updateTimers(delta: Float): Creature = {
    this.modify(_.params.animationTimer).using(_.update(delta))
  }

  def copy(params: Params = params): Creature

}

object Creature {
  case class Params(
    id: String,
    posX: Float,
    posY: Float,
    facingDirection: Direction.Value = Direction.Down,
    animationTimer: SimpleTimer = SimpleTimer(),
    isMoving: Boolean = false,
    areaId: String
  )
}
