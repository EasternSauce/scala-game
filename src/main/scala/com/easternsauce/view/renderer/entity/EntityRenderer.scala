package com.easternsauce.view.renderer.entity

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.{Animation, Sprite, TextureAtlas, TextureRegion}
import com.badlogic.gdx.math.Rectangle
import com.easternsauce.model.GameState
import com.easternsauce.util.Direction.Direction
import com.easternsauce.util.RendererBatch
import com.easternsauce.view.renderer.RendererController

case class EntityRenderer(gameView: RendererController, creatureId: String, atlas: TextureAtlas) {

  val sprite: Sprite = new Sprite()

  val facingTextures: Array[TextureRegion] = new Array[TextureRegion](4)

  val runningAnimations: Array[Animation[TextureRegion]] = new Array[Animation[TextureRegion]](4)

  var textureRegion: TextureRegion = _

  var abilityComponentRenderers: List[AbilityComponentRenderer] = _

  def init(gameState: GameState): Unit = {
    val creature = gameState.creatures(creatureId)

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

    abilityComponentRenderers =
      (for ((abilityId, ability) <- creature.params.abilities; (componentId, _) <- ability.components)
        yield AbilityComponentRenderer(gameView, creatureId, abilityId, componentId, atlas)).toList

    abilityComponentRenderers.foreach(_.init(gameState))

  }

  def runningAnimation(gameState: GameState, currentDirection: Direction): TextureRegion = {
    val creature = gameState.creatures(creatureId)

    runningAnimations(creature.dirMap(currentDirection))
      .getKeyFrame(gameState.player.params.animationTimer.time, true)
  }

  def facingTexture(gameState: GameState, currentDirection: Direction): TextureRegion = {
    val creature = gameState.creatures(creatureId)

    facingTextures(creature.dirMap(currentDirection))
  }

  def update(gameState: GameState): Unit = {

    val creature = gameState.creatures(creatureId)
    val texture =
      if (!creature.params.isMoving) facingTexture(gameState, creature.params.facingDirection)
      else runningAnimation(gameState, creature.params.facingDirection)
    sprite.setRegion(texture)
    sprite.setCenter(creature.params.posX, creature.params.posY)
    sprite.setSize(creature.width, creature.height)

    if (!creature.isAlive) {
      sprite.setOriginCenter()
      sprite.setRotation(90f)
    }

    abilityComponentRenderers.foreach(_.update(gameState))

  }

  def render(batch: RendererBatch): Unit = {
    sprite.draw(batch.spriteBatch)
  }

  def renderAbilities(gameState: GameState, batch: RendererBatch): Unit = {
    abilityComponentRenderers.foreach(_.render(gameState, batch))
  }

  def renderLifeBar(batch: RendererBatch, gameState: GameState): Unit = {
    val lifeBarHeight = 0.16f
    val lifeBarWidth = 2.0f

    val creature = gameState.creatures(creatureId)

    val currentLifeBarWidth = lifeBarWidth * creature.params.life / creature.params.maxLife
    val barPosX = creature.params.posX - lifeBarWidth / 2
    val barPosY = creature.params.posY + sprite.getWidth / 2 + 0.3125f

    batch
      .filledRectangle(new Rectangle(barPosX, barPosY, lifeBarWidth, lifeBarHeight), Color.ORANGE)
    batch
      .filledRectangle(new Rectangle(barPosX, barPosY, currentLifeBarWidth, lifeBarHeight), Color.RED)

  }
}
