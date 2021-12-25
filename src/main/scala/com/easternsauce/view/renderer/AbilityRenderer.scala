package com.easternsauce.view.renderer

import com.badlogic.gdx.graphics.g2d.{Animation, Sprite, TextureAtlas, TextureRegion}
import com.easternsauce.model.GameState
import com.easternsauce.model.creature.ability.AbilityState
import com.easternsauce.util.RendererBatch
import com.easternsauce.view.GameView

case class AbilityRenderer(gameView: GameView, creatureId: String, abilityId: String, atlas: TextureAtlas) {
  val sprite: Sprite = new Sprite()
  var channelAnimation: Animation[TextureRegion] = _
  var activeAnimation: Animation[TextureRegion] = _

  var channelTextureRegion: TextureRegion = _
  var activeTextureRegion: TextureRegion = _

  def init(gameState: GameState): Unit = {
    val ability = gameState.abilities(creatureId, abilityId)

    channelTextureRegion = atlas.findRegion(ability.channelSpriteType)
    activeTextureRegion = atlas.findRegion(ability.activeSpriteType)

    val channelFrames = for { i <- (0 until ability.channelFrameCount).toArray } yield {
      new TextureRegion(channelTextureRegion, i * ability.textureWidth, 0, ability.textureWidth, ability.textureHeight)
    }
    channelAnimation = new Animation[TextureRegion](ability.channelFrameDuration, channelFrames: _*)

    val activeFrames = for { i <- (0 until ability.activeFrameCount).toArray } yield {
      new TextureRegion(activeTextureRegion, i * ability.textureWidth, 0, ability.textureWidth, ability.textureHeight)
    }
    activeAnimation = new Animation[TextureRegion](ability.activeFrameDuration, activeFrames: _*)

  }

  def update(gameState: GameState): Unit = {
    val ability = gameState.abilities(creatureId, abilityId)
    val abilityState = ability.params.state

    def updateSprite(texture: TextureRegion): Unit = {
      sprite.setRegion(texture)
      sprite.setSize(ability.params.abilityHitbox.width, ability.params.abilityHitbox.height)
      sprite.setCenter(ability.params.abilityHitbox.x, ability.params.abilityHitbox.y)
      sprite.setOriginCenter()
      sprite.setRotation(ability.params.abilityHitbox.rotationAngle)
      sprite.setScale(ability.params.abilityHitbox.scale)
    }

    if (abilityState == AbilityState.Channeling) {
      val texture =
        channelAnimation.getKeyFrame(ability.params.abilityChannelAnimationTimer.time)
      updateSprite(texture)

    }

    if (abilityState == AbilityState.Active) {
      val texture =
        activeAnimation.getKeyFrame(ability.params.abilityActiveAnimationTimer.time)
      updateSprite(texture)

    }

  }

  def render(gameState: GameState, batch: RendererBatch): Unit = {
    val state = gameState.abilities(creatureId, abilityId).params.state

    if (state == AbilityState.Channeling || state == AbilityState.Active) {
      sprite.draw(batch.spriteBatch)
    }
  }

}
