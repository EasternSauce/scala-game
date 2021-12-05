package com.easternsauce.physics

import com.badlogic.gdx.maps.tiled.TiledMap
import com.easternsauce.model.GameState
import com.easternsauce.physics.creature.EntityBody
import com.easternsauce.physics.terrain.Terrain

case class PhysicsController() {
  var entityBodies: Map[String, EntityBody] = Map()
  var terrain: Map[String, Terrain] = Map()

  def init(gameState: GameState, maps: Map[String, TiledMap], mapScale: Float): Unit = {

    terrain = maps.map { case (areaId, map) => areaId -> Terrain(map, mapScale) }

    terrain.values.foreach(_.init())

    entityBodies = gameState.creatures.keys.map(creatureId => creatureId -> EntityBody(creatureId)).toMap

    entityBodies.values.foreach(entityBody => {

      val areaId = gameState.creatures(entityBody.id).params.areaId

      entityBody.init(gameState = gameState, physicsController = this, areaId = areaId)
    })
  }

  def processCreatureAreaChanges(gameState: GameState, areaChangeQueue: List[(String, String, String)]): Unit = {
    areaChangeQueue.foreach {
      case (creatureId, oldAreaId, newAreaId) =>
        terrain(oldAreaId).world.destroyBody(entityBodies(creatureId).body)
        entityBodies(creatureId).init(gameState = gameState, physicsController = this, areaId = newAreaId)
    }
  }

  def dispose(): Unit = terrain.values.foreach(_.dispose())
}
