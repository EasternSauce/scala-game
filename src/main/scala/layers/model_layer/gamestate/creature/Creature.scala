package layers.model_layer.gamestate.creature

import com.softwaremill.quicklens.ModifyPimp
import data.{AnimationData, SpriteTextureData}
import layers.model_layer.gamestate.creature.Creature.Params
import util.{Direction, SimpleTimer}

abstract class Creature {
  val isPlayer = false

  val params: Params

  def update(delta: Float): Creature = {
    this.updateTimers(delta)
  }

  def updateTimers(delta: Float): Creature = {
    this.modify(_.params.animationTimer).using(_.update(delta))
  }

  def copy(params: Params = params): Creature

}

object Creature {
  case class Params(
    id: String,
    posX: Float,
    posY: Float,
    spriteTextureData: SpriteTextureData,
    animationData: AnimationData,
    facingDirection: Direction.Value = Direction.Down,
    animationTimer: SimpleTimer = SimpleTimer(),
    isMoving: Boolean = false
  )
}
