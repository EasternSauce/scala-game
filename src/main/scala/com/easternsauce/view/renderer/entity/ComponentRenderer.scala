package com.easternsauce.view.renderer.entity

import com.badlogic.gdx.graphics.g2d.{Animation, Sprite, TextureAtlas, TextureRegion}
import com.easternsauce.model.GameState
import com.easternsauce.model.creature.ability.AbilityState
import com.easternsauce.util.RendererBatch

case class ComponentRenderer(creatureId: String, abilityId: String, componentId: String, atlas: TextureAtlas) {
  val sprite: Sprite = new Sprite()
  var channelAnimation: Animation[TextureRegion] = _
  var activeAnimation: Animation[TextureRegion] = _

  var channelTextureRegion: TextureRegion = _
  var activeTextureRegion: TextureRegion = _

  def init(gameState: GameState): Unit = {
    val abilityComponent = gameState.abilities(creatureId, abilityId).components(componentId)

    channelTextureRegion = atlas.findRegion(abilityComponent.specification.channelSpriteType)
    activeTextureRegion = atlas.findRegion(abilityComponent.specification.activeSpriteType)

    val channelFrames = for { i <- (0 until abilityComponent.specification.channelFrameCount).toArray } yield {
      new TextureRegion(
        channelTextureRegion,
        i * abilityComponent.specification.textureWidth,
        0,
        abilityComponent.specification.textureWidth,
        abilityComponent.specification.textureHeight
      )
    }
    channelAnimation = new Animation[TextureRegion](
      abilityComponent.specification.channelFrameDuration / abilityComponent.params.speed,
      channelFrames: _*
    )

    val activeFrames = for { i <- (0 until abilityComponent.specification.activeFrameCount).toArray } yield {
      new TextureRegion(
        activeTextureRegion,
        i * abilityComponent.specification.textureWidth,
        0,
        abilityComponent.specification.textureWidth,
        abilityComponent.specification.textureHeight
      )
    }
    activeAnimation = new Animation[TextureRegion](
      abilityComponent.specification.activeFrameDuration / abilityComponent.params.speed,
      activeFrames: _*
    )

  }

  def update(gameState: GameState): Unit = {
    val ability = gameState.abilities(creatureId, abilityId)
    val abilityComponent = gameState.abilities(creatureId, abilityId).components(componentId)
    val abilityState = abilityComponent.params.state

    def updateSprite(texture: TextureRegion): Unit = {
      sprite.setRegion(texture)
      sprite.setSize(abilityComponent.params.renderWidth, abilityComponent.params.renderHeight)
      sprite.setCenter(abilityComponent.params.renderPos.x, abilityComponent.params.renderPos.y)
      sprite.setOriginCenter()
      sprite.setRotation(abilityComponent.params.renderRotation)
      sprite.setScale(abilityComponent.params.renderScale)
    }

    if (abilityState == AbilityState.Channel) {
      val texture =
        channelAnimation.getKeyFrame(
          abilityComponent.params.abilityChannelAnimationTimer.time,
          ability.specification.channelAnimationLooping
        )
      updateSprite(texture)

    }

    if (abilityState == AbilityState.Active) {
      val texture =
        activeAnimation.getKeyFrame(
          abilityComponent.params.abilityActiveAnimationTimer.time,
          ability.specification.activeAnimationLooping
        )
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
