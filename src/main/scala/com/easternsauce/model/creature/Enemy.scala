package com.easternsauce.model.creature

import com.easternsauce.model.GameState

import scala.util.chaining.scalaUtilChainingOps

abstract class Enemy(override val params: CreatureParams) extends Creature {

  override val isEnemy: Boolean = true

  override val isControlledAutomatically = true

  override def updateAutomaticControls(gameState: GameState): Enemy = {

    val potentialTarget = gameState.player // TODO: look for closest target instead

    val vectorTowardsTarget = pos.vectorTowards(potentialTarget.pos)

    if (isAlive && potentialTarget.isAlive) {
      this
        .pipe(
          creature =>
            if (potentialTarget.pos.distance(creature.pos) > 3f && potentialTarget.pos.distance(creature.pos) < 15f)
              creature.moveInDir(vectorTowardsTarget).asInstanceOf[Enemy]
            else creature.stopMoving().asInstanceOf[Enemy]
        )
        .pipe(
          creature =>
            if (potentialTarget.pos.distance(creature.pos) < 4f)
              creature.attack(vectorTowardsTarget).asInstanceOf[Enemy]
            else creature
        )
    } else this.stopMoving().asInstanceOf[Enemy]

  }

  override def copy(params: CreatureParams): Enemy = {
    // unreachable, always overriden; needed for quicklens to work in abstract class
    ???
  }
}
