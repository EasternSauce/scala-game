package com.easternsauce.view.renderer.terrain

import com.badlogic.gdx.graphics.g2d.{Sprite, TextureRegion}
import com.easternsauce.model.GameState
import com.easternsauce.system.Assets
import com.easternsauce.util.RendererBatch

case class LootPileRenderer(areaId: String, lootPileId: String) {

  val sprite: Sprite = new Sprite()

  private val spriteWidth: Float = 1.2f
  private val spriteHeight: Float = 1.2f

  var textureRegion: TextureRegion = _

  def init(gameState: GameState): Unit = {
    val lootPile = gameState.areas(areaId).params.lootPiles(lootPileId)

    if (!lootPile.isTreasure) {
      sprite.setRegion(Assets.atlas.findRegion("bag"))
    } else
      sprite.setRegion(Assets.atlas.findRegion("treasure"))

    sprite.setSize(spriteWidth, spriteHeight)
    sprite.setCenter(lootPile.x, lootPile.y)
  }

  def render(gameState: GameState, batch: RendererBatch): Unit = {
    sprite.draw(batch.spriteBatch)
  }

}
