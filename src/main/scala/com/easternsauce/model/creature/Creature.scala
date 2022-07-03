package com.easternsauce.model.creature

import com.easternsauce.helper.InventoryWindowHelper
import com.easternsauce.model.GameState
import com.easternsauce.model.creature.ability.sword.SwingWeaponAbility
import com.easternsauce.model.creature.ability.{Ability, AbilityComponent, AbilityState}
import com.easternsauce.model.creature.effect.Effect
import com.easternsauce.model.item.Item
import com.easternsauce.model.util.EnhancedChainingSyntax.enhancedScalaUtilChainingOps
import com.easternsauce.system.Random
import com.easternsauce.util.Direction.Direction
import com.easternsauce.util.{Direction, InventoryMapping, Vec2}
import com.softwaremill.quicklens._

abstract class Creature {
  val isPlayer = false
  val isEnemy = false

  val params: CreatureParams
  val spriteType: String
  val textureWidth: Int
  val textureHeight: Int
  val width: Float
  val height: Float
  val frameDuration: Float
  val frameCount: Int
  val neutralStanceFrame: Int
  val dirMap: Map[Direction, Int]
  val baseLife: Float
  val baseStamina: Float = 100f

  val isControlledAutomatically: Boolean = false

  protected val staminaRegenerationTickTime = 0.005f
  protected val staminaRegeneration = 0.8f
  protected val staminaOveruseTime = 2f
  protected val staminaRegenerationDisabled = 1.2f

  def creatureId: String = this.params.id

  val defaultAbilityId = "swingWeapon"

  val speed: Float = 15f

  val unarmedDamage: Int = 10

  val dropTable: Map[String, Float] = Map()

  val onGettingHitSoundId: Option[String] = None

  val abilityUsages: Map[String, AbilityUsage] = Map()

  var useAbilityTimeout: Float = 4

  val abilities: List[Ability] = List(SwingWeaponAbility())

  def weaponDamage: Int =
    params.equipmentItems.get(InventoryMapping.primaryWeaponIndex).flatMap(_.damage).getOrElse(unarmedDamage)

  def init(): Creature = {
    this
      .initAbilities(abilities)
      .modifyAll(_.params.maxLife, _.params.life)
      .setTo(baseLife)
      .modifyAll(_.params.maxStamina, _.params.stamina)
      .setTo(baseStamina)
      .modify(_.params.inbetweenAbilitiesTime)
      .setTo(Random.between(2f, 6f))
  }

  def initAbilities(abilities: List[Ability]): Creature = {
    this
      .modify(_.params.abilities)
      .setTo(abilities.map(ability => ability.params.id -> ability.init()).toMap)
  }

  def update(delta: Float): Creature = {
    this
      .updateTimers(delta)
      .updateStamina(delta)
      .modify(_.params.effects)
      .using(_.map { case (name, effect) => (name, effect.update(delta)) })
  }

  def updateTimers(delta: Float): Creature = {
    this
      .modifyAll(
        _.params.animationTimer,
        _.params.staminaOveruseTimer,
        _.params.staminaRegenerationTimer,
        _.params.staminaRegenerationDisabledTimer,
        _.params.pathCalculationCooldownTimer,
        _.params.useAbilityTimer
      )
      .using(_.update(delta))
  }

  def setPosition(newPosX: Float, newPosY: Float): Creature = {
    this
      .modify(_.params.posX)
      .setTo(newPosX)
      .modify(_.params.posY)
      .setTo(newPosY)
  }

  def pos: Vec2 = Vec2(params.posX, params.posY)

  def takeStaminaDamage(staminaDamage: Float): Creature = {
    if (params.stamina - staminaDamage > 0) this.modify(_.params.stamina).setTo(this.params.stamina - staminaDamage)
    else {
      this
        .modify(_.params.stamina)
        .setTo(0f)
        .modify(_.params.staminaOveruse)
        .setTo(true)
        .modify(_.params.staminaOveruseTimer)
        .using(_.restart())
    }
  }

  def modifyAbility(abilityId: String)(operation: Ability => Ability): Creature =
    this
      .modify(_.params.abilities.at(abilityId))
      .using(operation)

  def modifyAbilityComponent(abilityId: String, componentId: String)(
    operation: AbilityComponent => AbilityComponent
  ): Creature =
    this
      .modify(_.params.abilities.at(abilityId).components.at(componentId))
      .using(operation)

  def updateStamina(delta: Float): Creature = {
    this
      .pipeIf(this.params.isSprinting && this.params.stamina > 0)(
        _.modify(_.params.staminaDrainTimer).using(_.update(delta))
      )
      .pipeIf(!params.isStaminaRegenerationDisabled && !this.params.isSprinting)(
        creature =>
          if (
            creature.params.staminaRegenerationTimer.time > creature.staminaRegenerationTickTime /* && !abilityActive */ && !creature.params.staminaOveruse
          ) {
            creature
              .pipe(creature => {
                val afterRegeneration = creature.params.stamina + creature.staminaRegeneration
                creature
                  .modify(_.params.stamina)
                  .setToIf(creature.params.stamina < creature.params.maxStamina)(
                    Math.min(afterRegeneration, creature.params.maxStamina)
                  )
              })
              .modify(_.params.staminaRegenerationTimer)
              .using(_.restart())

          } else creature
      )
      .pipe(
        creature =>
          creature
            .modify(_.params.staminaOveruse)
            .setToIf(
              creature.params.staminaOveruse && creature.params.staminaOveruseTimer.time > creature.staminaOveruseTime
            )(false)
      )
      .pipe(
        _.modify(_.params.isStaminaRegenerationDisabled)
          .setToIf(params.staminaRegenerationDisabledTimer.time > staminaRegenerationDisabled)(false)
      )
  }

  def onDeath(): Creature = {
    this.stopMoving()
  }

  def isAlive: Boolean = params.life > 0f

  def activateEffect(effect: String, time: Float): Creature = {
    if (params.effects.contains(effect)) {
      this.modify(_.params.effects.at(effect)).using(_.activate(time))
    } else {
      this.modify(_.params.effects).setTo(this.params.effects + (effect -> Effect(effect).activate(time)))
    }
  }

  def isEffectActive(effect: String): Boolean = {
    params.effects.contains(effect) && params.effects(effect).isActive
  }

  def ableToMove: Boolean = !this.isEffectActive("stagger") && !this.isEffectActive("knockback") && this.isAlive

  def startMoving(): Creature =
    this.modify(_.params.currentSpeed).setTo(this.speed).modify(_.params.animationTimer).using(_.restart())

  def stopMoving(): Creature = this.modify(_.params.currentSpeed).setTo(0f)

  def moveInDir(dir: Vec2): Creature = this.modify(_.params.movingDir).setTo(dir).startMoving()

  def isMoving: Boolean = this.params.currentSpeed > 0f

  def updateAutomaticControls(gameState: GameState): GameState = gameState

  def facingDirection: Direction = {
    val movingDir = params.movingDir
    movingDir.angleDeg() match {
      case angle if angle >= 45 && angle < 135  => Direction.Up
      case angle if angle >= 135 && angle < 225 => Direction.Left
      case angle if angle >= 225 && angle < 315 => Direction.Down
      case _                                    => Direction.Right
    }
  }

  def attack(gameState: GameState, dir: Vec2): GameState =
    gameState
      .modifyGameStateCreature(creatureId)(_.modify(_.params.actionDirVector).setTo(dir))
      .pipe(gameState => gameState.performAbility(creatureId, defaultAbilityId))

  def performAbility(gameState: GameState, abilityId: String): GameState = {

    val ability = this.params.abilities(abilityId)

    if (
      this.params.stamina > 0 && (ability.specification.isEmpty || !ability.componentsActive) && !ability.onCooldown
      /*&& !creature.abilityActive*/
    ) {
      ability.components.keys
        .foldLeft(gameState)((gameState, componentId) => {
          gameState
            .modifyGameStateAbilityComponent(creatureId, abilityId, componentId) {
              _.modify(_.params.channelTimer)
                .using(_.restart())
                .modify(_.params.state)
                .setTo(AbilityState.DelayedStart)
            }
        })
        .modifyGameStateCreature(creatureId = creatureId) {
          _.modify(_.params.staminaRegenerationDisabledTimer)
            .using(_.restart())
            .modify(_.params.isStaminaRegenerationDisabled)
            .setTo(true)
            .takeStaminaDamage(15f)
            .modifyAbility(abilityId) {
              _.modify(_.params.abilityTimer)
                .using(_.restart())
            }
        }
        .pipe(gameState.abilities(creatureId, abilityId).onStart(creatureId))

    } else gameState
  }

  def canPickUpItem(item: Item): Boolean = {
    val template = item.template
    val stackable: Boolean = template.stackable.get

    val inventoryFull = (0 until InventoryWindowHelper.inventoryTotalSlots).forall(params.inventoryItems.contains)

    val canStack = if (stackable) {
      val itemToStack: Option[(Int, Item)] = params.inventoryItems.find {
        case (_, item) => item.template.id == template.id
      }

      itemToStack.nonEmpty // TODO: limited quantity per slot?
    } else false

    if (inventoryFull) {
      canStack
    } else true
  }

  def pickUpItem(item: Item): Creature = {

    val template = item.template
    val stackable: Boolean = template.stackable.get

//    val inventoryFull = (0 until InventoryWindowHelper.inventoryTotalSlots).forall(params.inventoryItems.contains)

    val itemToStack: Option[(Int, Item)] = params.inventoryItems.find {
      case (_, item) => item.template.id == template.id
    }

    if (stackable && itemToStack.nonEmpty) { // if we can stack with existing item

      val (i, _) = itemToStack.get
      this.modify(_.params.inventoryItems.at(i).quantity).using(_ + 1)

    } else {
      val freeSlot = (0 until InventoryWindowHelper.inventoryTotalSlots).find(!params.inventoryItems.contains(_))

      if (freeSlot.isEmpty) throw new RuntimeException("unable to pick up item")

      this.modify(_.params.inventoryItems).setTo(this.params.inventoryItems + (freeSlot.get -> item))
    }
  }

  def capability: Int = {
    if (width >= 0 && width < 2) 1
    else if (width >= 2 && width <= 4) 2
    else if (width >= 4 && width <= 6) 3
    else 4
  }

  def copy(params: CreatureParams = params): Creature

}

case class AbilityUsage(
  weight: Float,
  minimumDistance: Float = 0f,
  maximumDistance: Float = Float.MaxValue,
  lifeThreshold: Float = 1.0f
)
