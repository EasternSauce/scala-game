package com.easternsauce.model.creature

import com.easternsauce.model.GameState
import com.easternsauce.model.creature.ability.sword.SwingWeaponAbility
import com.easternsauce.model.creature.ability.{Ability, AbilityComponent, AbilityState}
import com.easternsauce.model.creature.effect.Effect
import com.easternsauce.model.item.Item
import com.easternsauce.util.Direction.Direction
import com.easternsauce.util.{Direction, InventoryMapping, Vector2Wrapper}
import com.softwaremill.quicklens._

import scala.util.chaining.scalaUtilChainingOps

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

  val isControlledAutomatically: Boolean = false

  protected val staminaRegenerationTickTime = 0.005f
  protected val staminaRegeneration = 0.8f
  protected val staminaOveruseTime = 2f
  protected val staminaRegenerationDisabled = 1.2f

  val defaultAbility = "swingWeapon"

  val speed: Float = 15f

  val unarmedDamage: Int = 10

  def weaponDamage: Int =
    params.equipmentItems.get(InventoryMapping.primaryWeaponIndex).flatMap(_.damage).getOrElse(unarmedDamage)

  def init(): Creature = {
    val swingWeaponAbility = SwingWeaponAbility().init()

    def idAbilityPair(ability: Ability) = (ability.params.id -> ability)

    this
      .modify(_.params.abilities)
      .setTo(Map(idAbilityPair(swingWeaponAbility)))
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
      .modify(_.params.animationTimer)
      .using(_.update(delta))
      .modify(_.params.staminaOveruseTimer)
      .using(_.update(delta))
      .modify(_.params.staminaRegenerationTimer)
      .using(_.update(delta))
      .modify(_.params.staminaRegenerationDisabledTimer)
      .using(_.update(delta))
  }

  def setPosition(newPosX: Float, newPosY: Float): Creature = {
    this
      .modify(_.params.posX)
      .setTo(newPosX)
      .modify(_.params.posY)
      .setTo(newPosY)
  }

  def pos: Vector2Wrapper = Vector2Wrapper(params.posX, params.posY)

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
      .pipe(
        creature =>
          if (creature.params.isSprinting && creature.params.stamina > 0) {
            creature.modify(_.params.staminaDrainTimer).using(_.update(delta))
          } else creature
      )
      .pipe(
        creature =>
          if (!params.isStaminaRegenerationDisabled && !creature.params.isSprinting) {
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

  def startMoving(): Creature =
    this.modify(_.params.currentSpeed).setTo(this.speed).modify(_.params.animationTimer).using(_.restart())

  def stopMoving(): Creature = this.modify(_.params.currentSpeed).setTo(0f)

  def moveInDir(dir: Vector2Wrapper): Creature = this.modify(_.params.movingDir).setTo(dir).startMoving()

  def isMoving: Boolean = this.params.currentSpeed > 0f

  def updateAutomaticControls(gameState: GameState): Creature = this

  def facingDirection: Direction = {
    val movingDir = params.movingDir
    movingDir.angleDeg() match {
      case angle if angle >= 45 && angle < 135  => Direction.Up
      case angle if angle >= 135 && angle < 225 => Direction.Left
      case angle if angle >= 225 && angle < 315 => Direction.Down
      case _                                    => Direction.Right
    }
  }

  def attack(dir: Vector2Wrapper): Creature =
    this.modify(_.params.actionDirVector).setTo(dir).performAbility(defaultAbility)

  def performAbility(abilityId: String): Creature = {

    val ability = this.params.abilities(abilityId)

    if (
      this.params.stamina > 0 && !ability.componentsActive && !ability.onCooldown
      /*&& !creature.abilityActive*/
    ) {
      ability.components.keys
        .foldLeft(this)((creature, componentId) => {
          creature
            .modifyAbilityComponent(abilityId, componentId) {
              _.modify(_.params.channelTimer)
                .using(_.restart())
                .modify(_.params.state)
                .setTo(AbilityState.DelayedStart)
            }
        })
        .modify(_.params.staminaRegenerationDisabledTimer)
        .using(_.restart())
        .modify(_.params.isStaminaRegenerationDisabled)
        .setTo(true)
        .takeStaminaDamage(15f)
        .modifyAbility(abilityId) {
          _.onStart(this)
            .modify(_.params.abilityTimer)
            .using(_.restart())
        }

    } else this
  }

  def tryPickUpItem(item: Item): Boolean = { // TODO
//    val template: ItemTemplate = item.template
//    val stackable: Boolean = template.stackable.get
//
//    if (stackable) {
//      var foundFreeSlot: Int = -1
//      params.equipmentItems.foreach {
//        case (key, value) =>
//          if (foundFreeSlot == -1 && (value.template == template)) {
//            // stackable and same type item exists in inventory
//            foundFreeSlot = key
//            params.equipmentItems(foundFreeSlot).quantity =
//              params.equipmentItems(foundFreeSlot).quantity + item.quantity
//
//            return true
//          }
//      }
//      params.inventoryItems.foreach {
//        case (key, value) =>
//          if (foundFreeSlot == -1 && (value.template == template)) {
//            // stackable and same type item exists in inventory
//            foundFreeSlot = key
//            params.inventoryItems(foundFreeSlot).quantity =
//              params.inventoryItems(foundFreeSlot).quantity + item.quantity
//
//            return true
//          }
//      }
//    }
//    for (i <- 0 until inventoryWindow.inventoryTotalSlots) {
//      val lootPile = item.lootPile.get
//
//      if (!params.inventoryItems.contains(i)) { // if slot empty
//        params.inventoryItems += (i -> item)
//        lootPile match {
//          //          case treasure: Treasure => //register treasure picked up, dont spawn it again for this save
//          //            try {
//          //              val writer: FileWriter =
//          //                new FileWriter("saves/treasure_collected.sav", true)
//          //              val area: Area = item.lootPileBackref.area
//          //              writer.write(
//          //                "treasure " + area.id + " " + area.treasureList
//          //                  .indexOf(treasure) + "\n"
//          //              )
//          //              writer.close()
//          //            } catch {
//          //              case e: IOException =>
//          //                e.printStackTrace()
//          //            } TODO treasure saves
//          case _ =>
//        }
//
//        return true
//      }
//    }
//    false
    true
  }

  def copy(params: CreatureParams = params): Creature

}
