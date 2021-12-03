package com.easternsauce.view

import com.badlogic.gdx.graphics.g2d.{SpriteBatch, TextureAtlas}
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.physics.box2d.World
import com.easternsauce.model.GameState
import com.easternsauce.view.area.Area
import com.easternsauce.view.entity.Entity

case class GameView(atlas: TextureAtlas) {

  var entities: Map[String, Entity] = Map()
  var areas: Map[String, Area] = Map()

  def init(mapLoader: TmxMapLoader): Unit = {
    val area1DataDirectory = "assets/areas/area1"
    val area1 = Area("area1", area1DataDirectory, mapLoader)
    areas = areas + (area1.id -> area1)
  }

  def update(gameState: GameState, world: World): Unit = {
    val creatures = gameState.creatures

    creatures.keys.foreach { creatureId =>
      if (!entities.contains(creatureId)) {
        val newEntity = Entity(creatureId, areas("area1").world, this, atlas) // TODO: change this
        entities = entities + (creatureId -> newEntity)
        newEntity.init(gameState)
      }

      entities(creatureId).update(gameState)
    }

  }

  def render(gameState: GameState, spriteBatch: SpriteBatch): Unit = {

    val creatures = gameState.creatures

    creatures.keys.foreach { creatureId =>
      if (entities.contains(creatureId)) {
        entities(creatureId).render(spriteBatch)
      }
    }

  }

  def dispose(): Unit = {
    areas.values.foreach(_.dispose())
  }

}
