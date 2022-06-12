package com.easternsauce.model.creature

import com.easternsauce.model.creature.ability.Ability
import com.easternsauce.model.creature.effect.Effect
import com.easternsauce.model.item.{Item, ItemTemplate}
import com.easternsauce.model.util.SimpleTimer
import com.easternsauce.util.Vector2Wrapper
import com.softwaremill.quicklens.ModifyPimp

case class CreatureParams(
  id: String,
  posX: Float,
  posY: Float,
  animationTimer: SimpleTimer = SimpleTimer(),
  areaId: String,
  life: Float = 0f,
  maxLife: Float = 0f,
  stamina: Float = 0f,
  maxStamina: Float = 0f,
  abilities: Map[String, Ability] = Map(),
  actionDirVector: Vector2Wrapper = Vector2Wrapper(0, 0),
  staminaOveruse: Boolean = false,
  staminaOveruseTimer: SimpleTimer = SimpleTimer(),
  staminaRegenerationTimer: SimpleTimer = SimpleTimer(isRunning = true),
  isSprinting: Boolean = false,
  staminaDrainTimer: SimpleTimer = SimpleTimer(),
  isStaminaRegenerationDisabled: Boolean = false,
  staminaRegenerationDisabledTimer: SimpleTimer = SimpleTimer(),
  totalArmor: Float = 0f,
  equipmentItems: Map[Int, Item] = Map(),
  inventoryItems: Map[Int, Item] = Map(
    2 -> Item(ItemTemplate.templates("leatherArmor")),
    10 -> Item(ItemTemplate.templates("woodenSword")).modify(_.damage).setTo(Some(9999))
  ), // TODO: test item present, remove after
  effects: Map[String, Effect] = Map(),
  movingDir: Vector2Wrapper = Vector2Wrapper(1, 1),
  currentSpeed: Float = 0f,
  passedGateRecently: Boolean = false,
  knockbackDir: Vector2Wrapper = Vector2Wrapper(0, 0),
  knockbackVelocity: Float = 0f,
  targetCreatureId: Option[String] = None,
  pathTowardsTarget: Option[List[(Vector2Wrapper)]] = None,
  forcePathCalculation: Boolean = false,
  pathCalculationCooldownTimer: SimpleTimer = SimpleTimer()
)
