package com.easternsauce.view.entity

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d._
import com.badlogic.gdx.math.Rectangle
import com.easternsauce.model.GameState
import com.easternsauce.util.{Direction, RendererBatch}
import com.easternsauce.view.GameView

case class EntityRenderer(gameView: GameView, id: String, atlas: TextureAtlas) {

  val sprite: Sprite = new Sprite()

  var facingTextures: Array[TextureRegion] = new Array[TextureRegion](4)

  var runningAnimations: Array[Animation[TextureRegion]] = new Array[Animation[TextureRegion]](4)

  var textureRegion: TextureRegion = _

  def init(gameState: GameState): Unit = {
    val creature = gameState.creatures(id)

    textureRegion = atlas.findRegion(creature.spriteType)

    for (i <- 0 until 4) {
      facingTextures(i) = new TextureRegion(
        textureRegion,
        creature.neutralStanceFrame * creature.textureWidth,
        i * creature.textureHeight,
        creature.textureWidth,
        creature.textureHeight
      )

    }

    for (i <- 0 until 4) {
      val frames = for { j <- (0 until creature.frameCount).toArray } yield {
        new TextureRegion(
          textureRegion,
          j * creature.textureWidth,
          i * creature.textureHeight,
          creature.textureHeight,
          creature.textureHeight
        )
      }
      runningAnimations(i) = new Animation[TextureRegion](creature.frameDuration, frames: _*)

    }

  }

  def runningAnimation(gameState: GameState, currentDirection: Direction.Value): TextureRegion = {
    val creature = gameState.creatures(id)

    runningAnimations(creature.dirMap(currentDirection))
      .getKeyFrame(gameState.player.params.animationTimer.time, true)
  }

  def facingTexture(gameState: GameState, currentDirection: Direction.Value): TextureRegion = {
    val creature = gameState.creatures(id)

    facingTextures(creature.dirMap(currentDirection))
  }

  def update(gameState: GameState): Unit = {
    val creature = gameState.creatures(id)
    val texture =
      if (!creature.params.isMoving) facingTexture(gameState, creature.params.facingDirection)
      else runningAnimation(gameState, creature.params.facingDirection)
    sprite.setRegion(texture)
    sprite.setCenter(creature.params.posX, creature.params.posY)
    sprite.setSize(creature.width, creature.height)

  }

  def render(batch: RendererBatch): Unit = {
    sprite.draw(batch.spriteBatch)
  }

  def renderLifeBar(batch: RendererBatch, gameState: GameState): Unit = {
    val lifeBarHeight = 0.16f
    val lifeBarWidth = 2.0f

    val creature = gameState.creatures(id)

    val currentLifeBarWidth = lifeBarWidth * creature.params.life / creature.params.maxLife
    val barPosX = creature.params.posX - lifeBarWidth / 2
    val barPosY = creature.params.posY + sprite.getWidth / 2 + 0.3125f

    batch
      .filledRectangle(new Rectangle(barPosX, barPosY, lifeBarWidth, lifeBarHeight), Color.ORANGE)
    batch
      .filledRectangle(new Rectangle(barPosX, barPosY, currentLifeBarWidth, lifeBarHeight), Color.RED)

  }
}
