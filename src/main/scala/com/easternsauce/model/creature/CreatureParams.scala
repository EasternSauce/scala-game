package com.easternsauce.model.creature

import com.badlogic.gdx.math.Vector2
import com.easternsauce.model.creature.ability.{Ability, RegularAttack}
import com.easternsauce.model.util.SimpleTimer
import com.easternsauce.util.Direction

case class CreatureParams(
  id: String,
  posX: Float,
  posY: Float,
  facingDirection: Direction.Value = Direction.Down,
  animationTimer: SimpleTimer = SimpleTimer(),
  isMoving: Boolean = false,
  areaId: String,
  life: Float,
  maxLife: Float,
  abilities: Map[String, Ability] = Map("regularAttack" -> RegularAttack()),
  dirVector: Vector2 = new Vector2(0, 0)
)
