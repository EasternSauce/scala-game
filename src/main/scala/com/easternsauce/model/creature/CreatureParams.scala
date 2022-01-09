package com.easternsauce.model.creature

import com.easternsauce.model.creature.ability.{Ability, RegularAttack}
import com.easternsauce.model.item.{Item, ItemTemplate}
import com.easternsauce.model.util.SimpleTimer
import com.easternsauce.util.Direction.Direction
import com.easternsauce.util.{Direction, Vector2Wrapper}

case class CreatureParams(
  id: String,
  posX: Float,
  posY: Float,
  facingDirection: Direction = Direction.Down,
  animationTimer: SimpleTimer = SimpleTimer(),
  isMoving: Boolean = false,
  areaId: String,
  life: Float,
  maxLife: Float,
  stamina: Float,
  maxStamina: Float,
  abilities: Map[String, Ability] = Map("regularAttack" -> RegularAttack()),
  dirVector: Vector2Wrapper = Vector2Wrapper(0, 0),
  staminaOveruse: Boolean = false,
  staminaOveruseTimer: SimpleTimer = SimpleTimer(),
  staminaRegenerationTimer: SimpleTimer = SimpleTimer(isRunning = true),
  isSprinting: Boolean = false,
  staminaDrainTimer: SimpleTimer = SimpleTimer(),
  isStaminaRegenerationDisabled: Boolean = false,
  staminaRegenerationDisabledTimer: SimpleTimer = SimpleTimer(),
  totalArmor: Float = 0f,
  equipmentItems: Map[Int, Item] = Map(),
  inventoryItems: Map[Int, Item] =
    Map(2 -> Item(ItemTemplate.templates("leatherArmor"))) // TODO: test item present, remove after
)
