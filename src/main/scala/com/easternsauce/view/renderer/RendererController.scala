package com.easternsauce.view.renderer

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.math.Rectangle
import com.easternsauce.model.GameState
import com.easternsauce.model.event.{
  UpdateRendererOnEnemyDespawnEvent,
  UpdateRendererOnEnemySpawnEvent,
  UpdateRendererOnLootPileDespawnEvent,
  UpdateRendererOnLootPileSpawnEvent
}
import com.easternsauce.util.{RendererBatch, Vector2Wrapper}
import com.easternsauce.view.physics.terrain.AreaGateBody
import com.easternsauce.view.renderer.entity.EntityRenderer
import com.easternsauce.view.renderer.hud.{InventoryRenderer, LootPickupMenuRenderer}
import com.easternsauce.view.renderer.terrain.{AreaGateRenderer, AreaRenderer, LootPileRenderer}

case class RendererController(atlas: TextureAtlas) {

  var entityRenderers: Map[String, EntityRenderer] = Map()
  var areaRenderers: Map[String, AreaRenderer] = Map()
  var areaGateRenderers: List[AreaGateRenderer] = List()

  var lootPileRenderers: Map[(String, String), LootPileRenderer] = Map()

  var inventoryRenderer: InventoryRenderer = _

  var lootPickupMenuRenderer: LootPickupMenuRenderer = _

  def init(gameState: GameState, maps: Map[String, TiledMap], mapScale: Float, areaGates: List[AreaGateBody]): Unit = { // TODO: passing physics object here! should retrieve it from game state information

    entityRenderers = gameState.creatures.keys.map(creatureId => creatureId -> EntityRenderer(creatureId, atlas)).toMap

    entityRenderers.values.foreach(_.init(gameState))

    areaRenderers = maps.map { case (areaId, map) => areaId -> AreaRenderer(areaId, map, mapScale) }

    areaRenderers.values.foreach(_.init())

    inventoryRenderer = InventoryRenderer()

    lootPickupMenuRenderer = LootPickupMenuRenderer()

    areaGateRenderers = areaGates.map(AreaGateRenderer)

    val areaLootPileCombinations: List[(String, String)] = gameState.areas.toList.foldLeft(List[(String, String)]()) {
      case (acc, (k, v)) => acc ++ List().zipAll(v.params.lootPiles.keys.toList, k, "")
    }

    lootPileRenderers = areaLootPileCombinations.map {
      case (areaId, lootPileId) => (areaId, lootPileId) -> LootPileRenderer(areaId, lootPileId)
    }.toMap

    lootPileRenderers.values.foreach(_.init(gameState))
  }

  def update(gameState: GameState): Unit = {

    gameState.events.foreach {
      case UpdateRendererOnEnemySpawnEvent(creatureId) =>
        entityRenderers = entityRenderers + (creatureId -> {
          val renderer = EntityRenderer(creatureId, atlas)
          renderer.init(gameState)
          renderer
        })
      case UpdateRendererOnEnemyDespawnEvent(creatureId) =>
        entityRenderers = entityRenderers - creatureId

      case UpdateRendererOnLootPileSpawnEvent(areaId, lootPileId) =>
        lootPileRenderers = lootPileRenderers + ((areaId, lootPileId) -> {
          val renderer = LootPileRenderer(areaId, lootPileId)
          renderer.init(gameState)
          renderer
        })
      case UpdateRendererOnLootPileDespawnEvent(areaId, lootPileId) =>
        lootPileRenderers = lootPileRenderers - ((areaId, lootPileId))
      case _ =>
    }

    gameState.creatures.keys.foreach { creatureId =>
      entityRenderers(creatureId).update(gameState)
    }

  }

  def renderHud(gameState: GameState, batch: RendererBatch, mousePosition: Vector2Wrapper): Unit = {
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

    lootPickupMenuRenderer.render(gameState, batch, mousePosition)

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

  def renderLootPiles(gameState: GameState, batch: RendererBatch): Unit = {
    lootPileRenderers.values.foreach(_.render(gameState, batch))
  }

  def dispose(): Unit = {
    areaRenderers.values.foreach(_.dispose())
  }

}
