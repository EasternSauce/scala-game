package screen

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.{Gdx, Input, Screen}
import com.softwaremill.quicklens.ModifyPimp
import layers.model_layer.gamestate.GameState
import layers.model_layer.gamestate.creature.Player
import layers.view_layer.updater.GameUpdater
import util.Direction

class PlayScreen(batch: SpriteBatch, img: Texture, var gameState: GameState, var gameUpdater: GameUpdater)
    extends Screen {

  override def show(): Unit = {}

  def update(delta: Float): Unit = {
    updatePlayerPosition()

    updateGameState(
      gameState.modify(_.player)
      .using(_.update(delta))
    )

    gameUpdater.update(gameState)
  }

  private def updatePlayerPosition(): Unit = {
    val oldPosX = gameState.player.params.posX
    val oldPosY = gameState.player.params.posY

    val directionalSpeed: Float = {
      import Input.Keys._

      val sqrt2 = 1.4142135f
      val speed = 2.0f

      val directionalSpeed = List(W, S, A, D).map(Gdx.input.isKeyPressed(_)) match {
        case List(true, _, true, _) => speed / sqrt2
        case List(true, _, _, true) => speed / sqrt2
        case List(_, true, true, _) => speed / sqrt2
        case List(_, true, _, true) => speed / sqrt2
        case _                      => speed
      }
      directionalSpeed
    }

    val (newPosX: Float, newPosY: Float) = {
      import Input.Keys._

      val x: Float = List(A, D).map(Gdx.input.isKeyPressed(_)) match {
        case List(true, false) => oldPosX - directionalSpeed
        case List(false, true) => oldPosX + directionalSpeed
        case _                 => oldPosX
      }

      val y: Float = List(S, W).map(Gdx.input.isKeyPressed(_)) match {
        case List(true, false) => oldPosY - directionalSpeed
        case List(false, true) => oldPosY + directionalSpeed
        case _                 => oldPosY
      }

      (x, y)
    }

    val facingDirection = {
      import Input.Keys._
      List(W, S, A, D).map(Gdx.input.isKeyPressed(_)) match {
        case List(true, _, _, _) => Direction.Up
        case List(_, true, _, _) => Direction.Down
        case List(_, _, true, _) => Direction.Left
        case List(_, _, _, true) => Direction.Right
        case _                   => gameState.player.params.facingDirection
      }

    }

    val isMoving = {
      import Input.Keys._
      List(W, S, A, D).map(Gdx.input.isKeyPressed(_)) match {
        case List(true, _, _, _) => true
        case List(_, true, _, _) => true
        case List(_, _, true, _) => true
        case List(_, _, _, true) => true
        case _                   => false
      }

    }

    val wasMoving = gameState.player.params.isMoving
    val startMovingAction = (wasMoving, isMoving) match {
      case (false, true) => player: Player => {
        player.modify(_.params.animationTimer).using(_.restart())
      }
      case _ => player: Player => player
    }

    updateGameState(
      gameState
        .modify(_.player.params.posX)
        .setTo(newPosX)
        .modify(_.player.params.posY)
        .setTo(newPosY)
        .modify(_.player.params.facingDirection)
        .setTo(facingDirection)
        .modify(_.player.params.isMoving)
        .setTo(isMoving)
        .modify(_.player)
        .using(startMovingAction)
    )
  }

  def updateGameState(gameState: GameState): Unit = {
    this.gameState = gameState
  }

  override def render(delta: Float): Unit = {
    update(delta)

    ScreenUtils.clear(1, 1, 1, 1)
    batch.begin()
    batch.draw(img, 10, 10)

    gameUpdater.render(gameState, batch)

    batch.end()

  }

  override def resize(width: Int, height: Int): Unit = {}

  override def pause(): Unit = {}

  override def resume(): Unit = {}

  override def hide(): Unit = {}

  override def dispose(): Unit = {
    batch.dispose()
    img.dispose()
  }
}
