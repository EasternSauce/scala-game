package com.easternsauce.view.sound

import com.easternsauce.model.GameState
import com.easternsauce.model.event.PlaySoundEvent
import com.easternsauce.system.Assets

case class SoundManager() {
  def update(gameState: GameState): Unit = {
    gameState.events.foreach {
      case PlaySoundEvent(soundId: String) => Assets.sound(soundId).play(0.1f)
      case _                               =>
    }
  }

}
