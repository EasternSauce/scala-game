package com.easternsauce.view.entity

import com.badlogic.gdx.graphics.g2d.{SpriteBatch, TextureAtlas}
import com.badlogic.gdx.math.Vector2
import com.easternsauce.model.GameState
import com.easternsauce.view.GameView
import com.easternsauce.view.area.Area

case class Entity(id: String, gameView: GameView, atlas: TextureAtlas) {
  val body: EntityBody = EntityBody(id)
  val renderer: EntityRenderer = EntityRenderer(gameView, id, atlas)

  def init(gameState: GameState): Unit = {
    val area: Area = gameView.areas(gameState.creatures(id).params.areaId)

    body.init(gameState, area.world)
    renderer.init(gameState)
  }

  def update(gameState: GameState): Unit = renderer.update(gameState)

  def render(batch: SpriteBatch): Unit = renderer.render(batch)

  def pos: Vector2 = body.pos

  def setVelocity(velocity: Vector2): Unit = body.setVelocity(velocity)

}
