package com.easternsauce.view.physics

import com.easternsauce.event.{AreaChangeEvent, CollisionEvent}
import com.easternsauce.model.GameState
import com.easternsauce.view.physics.entity.EntityBody
import com.easternsauce.view.physics.terrain.{AreaGate, Terrain}

import scala.collection.mutable.ListBuffer

case class PhysicsController(terrains: Map[String, Terrain], areaGates: List[AreaGate]) {
  var entityBodies: Map[String, EntityBody] = Map()

  var collisionQueue: ListBuffer[CollisionEvent] = _

  def init(gameState: GameState): Unit = {

    terrains.values.foreach(_.init(collisionQueue))

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
        terrains(oldAreaId).world.destroyBody(entityBodies(creatureId).b2Body)
        entityBodies(creatureId).init(gameState = gameState, physicsController = this, areaId = newAreaId)
    }

  }

  def setCollisionQueue(collisionQueue: ListBuffer[CollisionEvent]): Unit = this.collisionQueue = collisionQueue

  def dispose(): Unit = terrains.values.foreach(_.dispose())
}
