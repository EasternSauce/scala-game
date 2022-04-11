package com.easternsauce.model.creature

import com.easternsauce.util.Vector2Wrapper
import com.softwaremill.quicklens.ModifyPimp

abstract class Enemy(override val params: CreatureParams) extends Creature {
  override def updateAutomaticControls(delta: Float): Enemy = {
    this.modify(_.params.velocity).setTo(Vector2Wrapper(1.0f, 1.0f))
  }

  override def copy(params: CreatureParams): Enemy = {
    // unreachable, needed for quicklens to work in abstract class
    ???
  }
}
