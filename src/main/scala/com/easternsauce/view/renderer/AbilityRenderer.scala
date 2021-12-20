package com.easternsauce.view.renderer

import com.badlogic.gdx.graphics.g2d.{Animation, Sprite, TextureAtlas, TextureRegion}
import com.easternsauce.model.GameState
import com.easternsauce.util.RendererBatch
import com.easternsauce.view.GameView

case class AbilityRenderer(gameView: GameView, creatureId: String, abilityId: String, atlas: TextureAtlas) {
  val sprite: Sprite = new Sprite()
  var channelAnimation: Animation[TextureRegion] = _
  var activeAnimation: Animation[TextureRegion] = _

  var channelTextureRegion: TextureRegion = _
  var activeTextureRegion: TextureRegion = _

  def init(gameState: GameState): Unit = {
    val creature = gameState.creatures(creatureId)

    val ability = gameState.abilities(creatureId, abilityId)

    channelTextureRegion = atlas.findRegion(ability.channelSpriteType)
    activeTextureRegion = atlas.findRegion(ability.activeSpriteType)

    val channelFrames = for { j <- (0 until ability.channelFrameCount).toArray } yield {
      new TextureRegion(
        channelTextureRegion,
        j * creature.textureWidth,
        0,
        creature.textureHeight,
        creature.textureHeight
      )
    }
    channelAnimation = new Animation[TextureRegion](ability.channelFrameDuration, channelFrames: _*)

    val activeFrames = for { j <- (0 until ability.activeFrameCount).toArray } yield {
      new TextureRegion(
        activeTextureRegion,
        j * creature.textureWidth,
        0,
        creature.textureHeight,
        creature.textureHeight
      )
    }
    activeAnimation = new Animation[TextureRegion](ability.activeFrameDuration, activeFrames: _*)

  }

  def update(gameState: GameState): Unit = {}

  def render(batch: RendererBatch): Unit = {
    //sprite.draw(batch.spriteBatch)
  }

}
