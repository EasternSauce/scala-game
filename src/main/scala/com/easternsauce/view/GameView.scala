package com.easternsauce.view

import com.badlogic.gdx.graphics.g2d.{SpriteBatch, TextureAtlas}
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.physics.box2d.World
import com.easternsauce.model.GameState
import com.easternsauce.view.area.AreaRenderer
import com.easternsauce.view.creature.CreatureRenderer

case class GameView(atlas: TextureAtlas) {

  var creatureRenderers: Map[String, CreatureRenderer] = Map()
  var areaRenderers: Map[String, AreaRenderer] = Map()

  def init(mapLoader: TmxMapLoader): Unit = {
    val area1DataDirectory = "assets/areas/area1"
    val area1 = AreaRenderer("area1", area1DataDirectory, mapLoader)
    areaRenderers = areaRenderers + (area1.id -> area1)

    area1.initPhysicalTerrain()

  }

  def update(gameState: GameState, world: World): Unit = {
    val creatures = gameState.creatures

    creatures.keys.foreach { creatureId =>
      if (!creatureRenderers.contains(creatureId)) {
        val newRenderer = CreatureRenderer(this, creatureId, atlas)
        creatureRenderers = creatureRenderers + (creatureId -> newRenderer)
        newRenderer.init(gameState, world)
      }

      creatureRenderers(creatureId).update(gameState, world)
    }

  }

  def render(gameState: GameState, spriteBatch: SpriteBatch): Unit = {

    val creatures = gameState.creatures

    creatures.keys.foreach { creatureId =>
      if (creatureRenderers.contains(creatureId)) {
        creatureRenderers(creatureId).render(spriteBatch)
      }
    }

  }

}
