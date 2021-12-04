package com.easternsauce.physics

import com.badlogic.gdx.maps.tiled.TiledMap
import com.easternsauce.model.GameState
import com.easternsauce.physics.area.Terrain
import com.easternsauce.physics.creature.EntityBody
import com.easternsauce.view.GameView

case class PhysicsController(gameView: GameView) {
  var entityBodies: Map[String, EntityBody] = Map()
  var terrain: Map[String, Terrain] = Map()

  def init(gameState: GameState, maps: Map[String, TiledMap], mapScale: Float): Unit = {

    terrain = maps.map { case (areaId, map) => areaId -> Terrain(map, mapScale) }

    terrain.values.foreach(_.init())

    entityBodies = gameState.creatures.keys.map(creatureId => creatureId -> EntityBody(creatureId)).toMap

    entityBodies.values.foreach(_.init(gameState, this))
  }
}
