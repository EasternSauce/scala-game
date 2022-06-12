package com.easternsauce.model.creature

import com.easternsauce.model.GameState
import com.softwaremill.quicklens.ModifyPimp

import scala.util.chaining.scalaUtilChainingOps

abstract class Enemy(override val params: CreatureParams) extends Creature {

  override val isEnemy: Boolean = true

  override val isControlledAutomatically = true

  val enemySearchDistance = 20f

  def findTarget(gameState: GameState): Option[Creature] = {
    val potentialTargets: List[Creature] = gameState.creatures.values.toList.filter(
      target =>
        target.params.areaId == this.params.areaId && target.isPlayer && target.pos
          .distance(this.pos) < enemySearchDistance
    )

    potentialTargets match {
      case List() => None
      case target => {
        Some(target.minBy(_.pos.distance(this.pos)))
      }
    }

  }

  override def updateAutomaticControls(gameState: GameState): Enemy = {

    val potentialTarget = findTarget(gameState)
    val potentialTargetId = potentialTarget.map(_.params.id)

    if (potentialTarget.nonEmpty && this.isAlive && potentialTarget.get.isAlive) {

      val vectorTowardsTarget = pos.vectorTowards(potentialTarget.get.pos)

      this
        .pipe(
          creature =>
            // target changed
            if (params.targetCreatureId.isEmpty || params.targetCreatureId != potentialTargetId) {
              creature
                .modify(_.params.forcePathCalculation)
                .setTo(true)
                .modify(_.params.targetCreatureId)
                .setTo(potentialTargetId)
                .modify(_.params.pathTowardsTarget)
                .setTo(None)
            } else creature
        )
        //        .modify(_.params.targetCreatureId)
        //        .setTo(potentialTarget.map(_.params.id))
        .pipe(creature => {
          if (
            potentialTarget.get.pos.distance(creature.pos) > 3f && potentialTarget.get.pos
              .distance(creature.pos) < enemySearchDistance
          ) {
            if (creature.params.pathTowardsTarget.nonEmpty && creature.params.pathTowardsTarget.get.nonEmpty) {
              val path = creature.params.pathTowardsTarget.get
              val nextNodeOnPath = path.head
              if (creature.pos.distance(nextNodeOnPath) < 0.3f) {
                creature.modify(_.params.pathTowardsTarget).setTo(Some(path.drop(1)))
              } else creature.moveInDir(creature.pos.vectorTowards(nextNodeOnPath))
            } else {
              creature.moveInDir(vectorTowardsTarget)
            }
          } else {
            creature
          }
        })
        .pipe(
          creature =>
            if (potentialTarget.get.pos.distance(creature.pos) < 1f)
              creature.attack(vectorTowardsTarget)
            else creature
        )
        .asInstanceOf[Enemy]
    } else this.modify(_.params.targetCreatureId).setTo(None).stopMoving().asInstanceOf[Enemy]

  }

  override def copy(params: CreatureParams): Enemy = {
    // unreachable, always overriden; needed for quicklens to work in abstract class
    ???
  }
}
