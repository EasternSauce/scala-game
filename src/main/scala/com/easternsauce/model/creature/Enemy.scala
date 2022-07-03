package com.easternsauce.model.creature

import com.easternsauce.model.GameState
import com.easternsauce.model.util.EnhancedChainingSyntax.enhancedScalaUtilChainingOps
import com.easternsauce.system.Random
import com.softwaremill.quicklens.ModifyPimp

abstract class Enemy(override val params: CreatureParams) extends Creature {

  override val isEnemy: Boolean = true

  override val isControlledAutomatically = true

  val enemySearchDistance = 30f

  def findTarget(gameState: GameState): Option[Creature] = {
    val potentialTargets: List[Creature] = gameState.creatures.values.toList.filter(
      target =>
        target.params.areaId == this.params.areaId && target.isPlayer && target.pos
          .distance(this.pos) < enemySearchDistance
    )

    potentialTargets match {
      case List() => None
      case target =>
        Some(target.minBy(_.pos.distance(this.pos)))
    }

  }

  override def updateAutomaticControls(gameState: GameState): GameState = {

    val potentialTarget = findTarget(gameState)
    val potentialTargetId = potentialTarget.map(_.params.id)

    if (potentialTarget.nonEmpty && this.isAlive && potentialTarget.get.isAlive) {

      val vectorTowardsTarget = pos.vectorTowards(potentialTarget.get.pos)

      val creature = gameState.creatures(creatureId)

      gameState
        .modifyGameStateCreature(creatureId) {
          _.pipeIf(params.targetCreatureId.isEmpty || params.targetCreatureId != potentialTargetId)(
            _.modify(_.params.forcePathCalculation)
              .setTo(true)
              .modify(_.params.targetCreatureId)
              .setTo(potentialTargetId)
              .modify(_.params.pathTowardsTarget)
              .setTo(None)
          )
          //        .modify(_.params.targetCreatureId)
          //        .setTo(potentialTarget.map(_.params.id))
            .pipeIf(
              potentialTarget.get.pos.distance(creature.pos) > 3f && potentialTarget.get.pos
                .distance(creature.pos) < enemySearchDistance
            ) { creature =>
              if (creature.params.pathTowardsTarget.nonEmpty && creature.params.pathTowardsTarget.get.nonEmpty) {
                val path = creature.params.pathTowardsTarget.get
                val nextNodeOnPath = path.head
                if (creature.pos.distance(nextNodeOnPath) < 2f) {
                  creature.modify(_.params.pathTowardsTarget).setTo(Some(path.drop(1)))
                } else creature.moveInDir(creature.pos.vectorTowards(nextNodeOnPath))
              } else {
                creature.moveInDir(vectorTowardsTarget)
              }
            }
        }
        .pipeIf(potentialTarget.get.pos.distance(creature.pos) < 3f) { gameState =>
          gameState.creatures(creatureId).attack(gameState, vectorTowardsTarget)
        }
        .pipe(gameState => {
          val creature = gameState.creatures(creatureId)

          val pickedAbilityId = pickAbilityToUse(gameState)
          if (
            params.useAbilityTimer.time > useAbilityTimeout + creature.params.inbetweenAbilitiesTime && abilityUsages.nonEmpty && pickedAbilityId.nonEmpty
          ) {
            gameState
              .modifyGameStateCreature(creatureId) {
                _.modify(_.params.actionDirVector)
                  .setTo(creature.pos.vectorTowards(potentialTarget.get.pos))
                  .pipe(_.modify(_.params.useAbilityTimer).using(_.restart()))
                  .modify(_.params.inbetweenAbilitiesTime)
                  .setTo(Random.between(2f, 6f))
              }
              .performAbility(creatureId, pickedAbilityId.get)

          } else gameState
        })
    } else
      gameState.modifyGameStateCreature(creatureId) { _.modify(_.params.targetCreatureId).setTo(None).stopMoving() }

  }

  def pickAbilityToUse(gameState: GameState): Option[String] = {

    if (params.targetCreatureId.nonEmpty) {
      val targetCreature = gameState.creatures(params.targetCreatureId.get)

      val filteredAbilityUsages = abilityUsages.filter {
        case (abilityId, usage) =>
          params.life / params.maxLife <= usage.lifeThreshold && pos.distance(
            targetCreature.pos
          ) > usage.minimumDistance &&
            pos.distance(targetCreature.pos) < usage.maximumDistance && !gameState
            .abilities(params.id, abilityId)
            .onCooldown
      }

      var completeWeight = 0.0f
      for (abilityUsage <- filteredAbilityUsages.values) {
        completeWeight += abilityUsage.weight
      }
      val r = Math.random * completeWeight
      var countWeight = 0.0
      for (abilityUsage <- filteredAbilityUsages) {
        val (key, value) = abilityUsage
        countWeight += value.weight
        if (countWeight > r) return Some(key)

      }
      None
    } else None

  }

  override def copy(params: CreatureParams): Enemy = {
    // unreachable, always overriden; needed for quicklens to work in abstract class
    this
  }
}
