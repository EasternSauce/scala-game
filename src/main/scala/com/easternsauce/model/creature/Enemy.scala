package com.easternsauce.model.creature

import com.easternsauce.model.GameState

import scala.util.chaining.scalaUtilChainingOps

abstract class Enemy(override val params: CreatureParams) extends Creature {

  override val isControlledAutomatically = true

  override def updateAutomaticControls(gameState: GameState): Enemy = {

    this.pipe(
      creature =>
        if (gameState.player.pos.distance(creature.pos) < 15f)
          creature.moveInDir(creature.pos.vectorTowards(gameState.player.pos)).asInstanceOf[Enemy]
        else creature.stopMoving().asInstanceOf[Enemy]
    )
  }

  override def copy(params: CreatureParams): Enemy = {
    // unreachable, always overriden; needed for quicklens to work in abstract class
    ???
  }
}
