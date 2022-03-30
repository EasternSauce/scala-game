package com.easternsauce.model.creature.effect

import com.easternsauce.model.util.SimpleTimer
import com.softwaremill.quicklens.ModifyPimp

import scala.util.chaining.scalaUtilChainingOps

case class Effect(name: String, endTime: Float = 0f, timer: SimpleTimer = SimpleTimer(), isActive: Boolean = false) {
  def update(delta: Float): Effect = {
    this
      .modify(_.timer)
      .using(_.update(delta))
      .pipe(
        effect =>
          if (effect.isActive && effect.timer.time > effect.endTime) effect.modify(_.isActive).setTo(false) else effect
      )

  }

  def stop(): Effect = {
    this.modify(_.isActive).setTo(false).modify(_.timer).using(_.stop()).modify(_.endTime).setTo(0)
  }

  def remainingTime: Float = endTime - timer.time

  def activate(effectTime: Float): Effect = {
    if (isActive) {
      this.modify(_.timer).using(_.restart()).modify(_.endTime).setTo(Math.max(remainingTime, effectTime))
    } else {
      this.modify(_.isActive).setTo(true).modify(_.timer).using(_.restart()).modify(_.endTime).setTo(effectTime)
    }
  }

}
