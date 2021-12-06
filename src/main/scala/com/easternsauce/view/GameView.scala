package com.easternsauce.view

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.physics.box2d.World
import com.easternsauce.model.GameState
import com.easternsauce.util.RendererBatch
import com.easternsauce.view.area.AreaRenderer
import com.easternsauce.view.entity.EntityRenderer

case class GameView(atlas: TextureAtlas) {

  var entityRenderers: Map[String, EntityRenderer] = Map()
  var areaRenderers: Map[String, AreaRenderer] = Map()

  def init(gameState: GameState, maps: Map[String, TiledMap], mapScale: Float): Unit = {

    entityRenderers =
      gameState.creatures.keys.map(creatureId => creatureId -> EntityRenderer(this, creatureId, atlas)).toMap

    entityRenderers.values.foreach(_.init(gameState))

    areaRenderers = maps.map { case (areaId, map) => areaId -> AreaRenderer(areaId, map, mapScale) }

    areaRenderers.values.foreach(_.init())

  }

  def update(gameState: GameState, world: World): Unit = {
    gameState.creatures.keys.foreach { creatureId =>
      entityRenderers(creatureId).update(gameState)
    }

  }

  def render(gameState: GameState, batch: RendererBatch): Unit = {

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

  def dispose(): Unit = {
    areaRenderers.values.foreach(_.dispose())
  }

}
