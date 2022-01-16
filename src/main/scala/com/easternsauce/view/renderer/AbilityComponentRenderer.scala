package com.easternsauce.view.renderer

import com.badlogic.gdx.graphics.g2d.{Animation, Sprite, TextureAtlas, TextureRegion}
import com.easternsauce.model.GameState
import com.easternsauce.model.creature.ability.AbilityState
import com.easternsauce.util.RendererBatch
import com.easternsauce.view.GameView

case class AbilityComponentRenderer(
  gameView: GameView,
  creatureId: String,
  abilityId: String,
  componentId: String,
  atlas: TextureAtlas
) {
  val sprite: Sprite = new Sprite()
  var channelAnimation: Animation[TextureRegion] = _
  var activeAnimation: Animation[TextureRegion] = _

  var channelTextureRegion: TextureRegion = _
  var activeTextureRegion: TextureRegion = _

  def init(gameState: GameState): Unit = {
    val abilityComponent = gameState.abilities(creatureId, abilityId).components(componentId)

    channelTextureRegion = atlas.findRegion(abilityComponent.channelSpriteType)
    activeTextureRegion = atlas.findRegion(abilityComponent.activeSpriteType)

    val channelFrames = for { i <- (0 until abilityComponent.channelFrameCount).toArray } yield {
      new TextureRegion(
        channelTextureRegion,
        i * abilityComponent.textureWidth,
        0,
        abilityComponent.textureWidth,
        abilityComponent.textureHeight
      )
    }
    channelAnimation = new Animation[TextureRegion](abilityComponent.channelFrameDuration, channelFrames: _*)

    val activeFrames = for { i <- (0 until abilityComponent.activeFrameCount).toArray } yield {
      new TextureRegion(
        activeTextureRegion,
        i * abilityComponent.textureWidth,
        0,
        abilityComponent.textureWidth,
        abilityComponent.textureHeight
      )
    }
    activeAnimation = new Animation[TextureRegion](abilityComponent.activeFrameDuration, activeFrames: _*)

  }

  def update(gameState: GameState): Unit = {
    val abilityComponent = gameState.abilities(creatureId, abilityId).components(componentId)
    val abilityState = abilityComponent.params.state

    def updateSprite(texture: TextureRegion): Unit = {
      sprite.setRegion(texture)
      sprite.setSize(abilityComponent.params.abilityHitbox.width, abilityComponent.params.abilityHitbox.height)
      sprite.setCenter(abilityComponent.params.abilityHitbox.x, abilityComponent.params.abilityHitbox.y)
      sprite.setOriginCenter()
      sprite.setRotation(abilityComponent.params.abilityHitbox.rotationAngle)
      sprite.setScale(abilityComponent.params.abilityHitbox.scale)
    }

    if (abilityState == AbilityState.Channel) {
      val texture =
        channelAnimation.getKeyFrame(abilityComponent.params.abilityChannelAnimationTimer.time)
      updateSprite(texture)

    }

    if (abilityState == AbilityState.Active) {
      val texture =
        activeAnimation.getKeyFrame(abilityComponent.params.abilityActiveAnimationTimer.time)
      updateSprite(texture)

    }

  }

  def render(gameState: GameState, batch: RendererBatch): Unit = {
    val state = gameState.abilities(creatureId, abilityId).components(componentId).params.state

    if (state == AbilityState.Channel || state == AbilityState.Active) {
      sprite.draw(batch.spriteBatch)
    }
  }

}
