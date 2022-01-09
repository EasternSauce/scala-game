package com.easternsauce.view

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.math.{Rectangle, Vector2}
import com.easternsauce.model.GameState
import com.easternsauce.util.RendererBatch
import com.easternsauce.view.renderer.{AreaRenderer, EntityRenderer, InventoryRenderer}

case class GameView(atlas: TextureAtlas) {

  var entityRenderers: Map[String, EntityRenderer] = Map()
  var areaRenderers: Map[String, AreaRenderer] = Map()

  var inventoryRenderer: InventoryRenderer = _

  def init(gameState: GameState, maps: Map[String, TiledMap], mapScale: Float): Unit = {

    entityRenderers =
      gameState.creatures.keys.map(creatureId => creatureId -> EntityRenderer(this, creatureId, atlas)).toMap

    entityRenderers.values.foreach(_.init(gameState))

    areaRenderers = maps.map { case (areaId, map) => areaId -> renderer.AreaRenderer(areaId, map, mapScale) }

    areaRenderers.values.foreach(_.init())

    inventoryRenderer = InventoryRenderer()

  }

  def update(gameState: GameState): Unit = {
    gameState.creatures.keys.foreach { creatureId =>
      entityRenderers(creatureId).update(gameState)
    }

  }

  def renderHud(gameState: GameState, batch: RendererBatch, mousePosition: Vector2): Unit = {
    def renderLifeAndStamina(): Unit = {
      val player = gameState.player

      val maxLifeRect = new Rectangle(10, 40, 100, 10)
      val lifeRect =
        new Rectangle(10, 40, 100 * player.params.life / player.params.maxLife, 10)
      val maxStaminaRect = new Rectangle(10, 25, 100, 10)
      val staminaRect =
        new Rectangle(10, 25, 100 * player.params.stamina / player.params.maxStamina, 10)

      batch.shapeDrawer.filledRectangle(maxLifeRect, Color.ORANGE)
      batch.shapeDrawer.filledRectangle(lifeRect, Color.RED)
      batch.shapeDrawer.filledRectangle(maxStaminaRect, Color.ORANGE)
      batch.shapeDrawer.filledRectangle(staminaRect, Color.GREEN)
    }

    inventoryRenderer.render(gameState, batch, mousePosition)

    renderLifeAndStamina()
  }

  def renderEntities(gameState: GameState, batch: RendererBatch): Unit = {

    gameState.creatures.keys.foreach { creatureId =>
      if (entityRenderers.contains(creatureId)) {
        entityRenderers(creatureId).render(batch)
      }
    }

    gameState.creatures.keys.foreach { creatureId =>
      if (entityRenderers.contains(creatureId)) {
        entityRenderers(creatureId).renderLifeBar(batch, gameState)
      }
    }

  }

  def renderAbilties(gameState: GameState, batch: RendererBatch): Unit = {

    gameState.creatures.keys.foreach { creatureId =>
      if (entityRenderers.contains(creatureId)) {
        entityRenderers(creatureId).renderAbilities(gameState, batch)
      }
    }

  }

  def dispose(): Unit = {
    areaRenderers.values.foreach(_.dispose())
  }

}
