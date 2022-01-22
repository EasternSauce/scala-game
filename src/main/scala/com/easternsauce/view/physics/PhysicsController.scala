package com.easternsauce.view.physics

import com.badlogic.gdx.maps.tiled.TiledMap
import com.easternsauce.view.physics.entity.EntityBody
import com.easternsauce.view.physics.terrain.Terrain
import com.easternsauce.event.{AreaChangeEvent, CollisionEvent}
import com.easternsauce.model.GameState

import scala.collection.mutable.ListBuffer

case class PhysicsController() {
  var entityBodies: Map[String, EntityBody] = Map()
  var terrain: Map[String, Terrain] = Map()

  var collisionQueue: ListBuffer[CollisionEvent] = _

  def init(gameState: GameState, maps: Map[String, TiledMap], mapScale: Float): Unit = {

    terrain = maps.map { case (areaId, map) => areaId -> Terrain(map, mapScale) }

    terrain.values.foreach(_.init(collisionQueue))

    entityBodies = gameState.creatures.keys.map(creatureId => creatureId -> EntityBody(creatureId)).toMap

    entityBodies.values.foreach(entityBody => {

      val areaId = gameState.creatures(entityBody.creatureId).params.areaId

      entityBody.init(gameState = gameState, physicsController = this, areaId = areaId)
    })
  }

  def update(gameState: GameState, areaChangeQueue: ListBuffer[AreaChangeEvent]): Unit = {
    entityBodies.values.foreach(_.update(gameState, this))

    areaChangeQueue.foreach {
      case AreaChangeEvent(creatureId, oldAreaId, newAreaId) =>
        terrain(oldAreaId).world.destroyBody(entityBodies(creatureId).b2Body)
        entityBodies(creatureId).init(gameState = gameState, physicsController = this, areaId = newAreaId)
    }

  }

  def setCollisionQueue(collisionQueue: ListBuffer[CollisionEvent]): Unit = this.collisionQueue = collisionQueue

  def dispose(): Unit = terrain.values.foreach(_.dispose())
}
