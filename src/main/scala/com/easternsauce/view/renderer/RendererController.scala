package com.easternsauce.view.renderer

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.math.{Rectangle, Vector2}
import com.easternsauce.model.GameState
import com.easternsauce.model.event.{EnemyDespawnEvent, EnemySpawnEvent}
import com.easternsauce.util.RendererBatch
import com.easternsauce.view.physics.terrain.AreaGatePair
import com.easternsauce.view.renderer.entity.EntityRenderer
import com.easternsauce.view.renderer.hud.InventoryRenderer
import com.easternsauce.view.renderer.terrain.{AreaGateRenderer, AreaRenderer}

case class RendererController(atlas: TextureAtlas) {

  var entityRenderers: Map[String, EntityRenderer] = Map()
  var areaRenderers: Map[String, AreaRenderer] = Map()
  var areaGateRenderers: List[AreaGateRenderer] = List()

  var inventoryRenderer: InventoryRenderer = _

  def init(gameState: GameState, maps: Map[String, TiledMap], mapScale: Float, areaGates: List[AreaGatePair]): Unit = {

    entityRenderers = gameState.creatures.keys.map(creatureId => creatureId -> EntityRenderer(creatureId, atlas)).toMap

    entityRenderers.values.foreach(_.init(gameState))

    areaRenderers = maps.map { case (areaId, map) => areaId -> AreaRenderer(areaId, map, mapScale) }

    areaRenderers.values.foreach(_.init())

    inventoryRenderer = InventoryRenderer()

    areaGateRenderers = areaGates.map(AreaGateRenderer)

  }

  def update(gameState: GameState): Unit = {

    if (gameState.events.nonEmpty) println(gameState.events)
    gameState.events.foreach {
      case EnemySpawnEvent(creatureId) =>
        entityRenderers = entityRenderers + (creatureId -> {
          val renderer = EntityRenderer(creatureId, atlas)
          renderer.init(gameState)
          renderer
        })
      case EnemyDespawnEvent(creature) =>
        entityRenderers = entityRenderers - creature.params.id
      case _ =>
    }

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
      if (
        entityRenderers.contains(creatureId) &&
        gameState.creatures(creatureId).params.areaId == gameState.currentAreaId
      ) {
        entityRenderers(creatureId).render(batch)
      }
    }

    gameState.creatures.keys.foreach { creatureId =>
      if (
        entityRenderers.contains(creatureId) &&
        gameState.creatures(creatureId).params.areaId == gameState.currentAreaId
      ) {
        entityRenderers(creatureId).renderLifeBar(batch, gameState)
      }
    }

  }

  def renderAbilities(gameState: GameState, batch: RendererBatch): Unit = {

    gameState.creatures.keys.foreach { creatureId =>
      if (entityRenderers.contains(creatureId)) {
        entityRenderers(creatureId).renderAbilities(gameState, batch)
      }
    }

  }

  def renderAreaGates(gameState: GameState, batch: RendererBatch): Unit = {
    areaGateRenderers.foreach(_.render(gameState, batch))
  }

  def dispose(): Unit = {
    areaRenderers.values.foreach(_.dispose())
  }

}
