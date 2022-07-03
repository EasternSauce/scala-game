package com.easternsauce.model.actions

import com.easternsauce.model.GameState
import com.easternsauce.model.creature.Creature
import com.easternsauce.model.creature.ability.{Ability, AbilityComponent}
import com.easternsauce.model.event.{PlaySoundEvent, UpdatePhysicsOnCreatureDeathEvent}
import com.easternsauce.model.util.EnhancedChainingSyntax.enhancedScalaUtilChainingOps
import com.easternsauce.util.Vec2
import com.easternsauce.view.pathfinding.Astar
import com.easternsauce.view.physics.PhysicsController
import com.softwaremill.quicklens._

trait CreatureActions {
  this: GameState =>

  def modifyGameStateCreature(creatureId: String)(operation: Creature => Creature): GameState = {

    this
      .modify(_.creatures.at(creatureId))
      .using(operation(_))

  }

  def modifyGameStateAbility(creatureId: String, abilityId: String)(operation: Ability => Ability): GameState = {

    this
      .modify(_.creatures.at(creatureId))
      .using(_.modifyAbility(abilityId)(operation(_)))

  }

  def modifyGameStateAbilityComponent(creatureId: String, abilityId: String, componentId: String)(
    operation: AbilityComponent => AbilityComponent
  ): GameState = {

    this
      .modify(_.creatures.at(creatureId))
      .using(_.modifyAbilityComponent(abilityId, componentId)(operation(_)))

  }

  def creatureTakeLifeDamage(creatureId: String, damage: Float, sourcePosX: Float, sourcePosY: Float): GameState = {
    val beforeLife = creatures(creatureId).params.life

    val actualDamage = damage * 100f / (100f + creatures(creatureId).params.totalArmor)

    val creature = this.creatures(creatureId)

    this
      .modify(_.events)
      .usingIf(creature.onGettingHitSoundId.nonEmpty)(_.prepended(PlaySoundEvent(creature.onGettingHitSoundId.get)))
      .modifyGameStateCreature(creatureId)(
        _.pipe(
          creature =>
            if (creature.params.life - actualDamage > 0)
              creature.modify(_.params.life).setTo(creature.params.life - actualDamage)
            else creature.modify(_.params.life).setTo(0f)
        ).activateEffect("knockback", 0.02f)
          .modify(_.params.knockbackDir)
          .setTo(
            Vec2(creatures(creatureId).params.posX - sourcePosX, creatures(creatureId).params.posY - sourcePosY).normal
          )
          .modify(_.params.knockbackVelocity)
          .setTo(20f)
      )
      .pipe(gameState => {
        val creature = gameState.creatures(creatureId)
        if (beforeLife > 0f && creature.params.life <= 0f) {
          gameState.creatureOnDeath(creatureId)
        } else gameState
      })
  }

  def creatureActivateEffect(creatureId: String, effectName: String, effectTime: Float): GameState = {
    this
      .modifyGameStateCreature(creatureId)(_.activateEffect(effectName, effectTime))
  }

  def creatureOnDeath(creatureId: String): GameState = {
    val creature = creatures(creatureId)

    val abilityComponentCombinations: List[(String, String)] =
      this.creatures(creatureId).params.abilities.toList.foldLeft(List[(String, String)]()) {
        case (acc, (k, v)) => acc ++ List().zipAll(v.components.keys.toList, k, "")
      }

    this
      .modify(_.events)
      .using(_.prepended(UpdatePhysicsOnCreatureDeathEvent(creatureId)))
      .modifyGameStateCreature(creatureId)(_.onDeath().modify(_.params.abilities.each).using(_.stop()))
      .pipe(gameState => {
        abilityComponentCombinations.foldLeft(gameState) {
          case (gameState, (abilityId, componentId)) =>
            gameState.onAbilityComponentInactiveStart(creatureId, abilityId, componentId)
        }
      })
      .spawnLootPile(creature.params.areaId, creature.params.posX, creature.params.posY, creature.dropTable)
  }

  def processPathfinding(physicsController: PhysicsController): GameState = {
    creatures.values
      .filter(
        creature =>
          creature.params.areaId == this.currentAreaId &&
            creature.isEnemy &&
            creature.params.targetCreatureId.nonEmpty &&
            (creature.params.forcePathCalculation || creature.params.pathCalculationCooldownTimer.time > 1f)
      )
      .foldLeft(this) {
        case (gameState, creature) =>
          val target = gameState.creatures(creature.params.targetCreatureId.get)
          val terrain = physicsController.terrains(creature.params.areaId)

          val isLineOfSight = terrain.isLineOfSight(creature.pos, target.pos)

          if (!isLineOfSight) {
            val path = Astar.findPath(terrain, creature.pos, target.pos, creature.capability)

            gameState.modifyGameStateCreature(creature.params.id)(
              _.modify(_.params.pathTowardsTarget)
                .setTo(Some(path))
                .modify(_.params.pathCalculationCooldownTimer)
                .using(_.restart())
                .modify(_.params.forcePathCalculation)
                .setTo(false)
            )
          } else gameState.modifyGameStateCreature(creature.params.id)(_.modify(_.params.pathTowardsTarget).setTo(None))

      }

  }
}
