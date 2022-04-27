package com.easternsauce.view.physics

import com.badlogic.gdx.physics.box2d._
import com.easternsauce.event.{AbilityComponentCollision, AreaGateCollision, LeftAreaGateEvent, PhysicsEvent}
import com.easternsauce.model.GameState
import com.easternsauce.model.event.{AreaChangeEvent, EnemyDespawnEvent, EnemySpawnEvent}
import com.easternsauce.view.physics.entity.{ComponentBody, EntityBody}
import com.easternsauce.view.physics.terrain.{AreaGateBody, AreaGatePair, Terrain}

import scala.collection.mutable.ListBuffer

case class PhysicsController(terrains: Map[String, Terrain], areaGates: List[AreaGatePair]) {
  var entityBodies: Map[String, EntityBody] = Map()

  def init(gameState: GameState, collisionQueue: ListBuffer[PhysicsEvent]): Unit = {

    terrains.values.foreach(terrain => {
      terrain.init()
      createContactListener(terrain.world, collisionQueue)
    })

    entityBodies = gameState.creatures.keys.map(creatureId => creatureId -> EntityBody(creatureId)).toMap

    entityBodies.values.foreach(entityBody => {

      val areaId = gameState.creatures(entityBody.creatureId).params.areaId

      entityBody.init(gameState = gameState, physicsController = this, areaId = areaId)
    })
  }

  def update(gameState: GameState): Unit = {

    gameState.events.foreach {
      case AreaChangeEvent(creatureId, oldAreaId, newAreaId, _, _) =>
        terrains(oldAreaId).world.destroyBody(entityBodies(creatureId).b2Body)
        entityBodies(creatureId).init(gameState = gameState, physicsController = this, areaId = newAreaId)
      case EnemySpawnEvent(creatureId) =>
        entityBodies = entityBodies + (creatureId -> {
          val entityBody = EntityBody(creatureId)
          entityBody.init(
            gameState = gameState,
            physicsController = this,
            areaId = gameState.creatures(creatureId).params.areaId
          )
          entityBody
        })
      case EnemyDespawnEvent(creature) =>
        terrains(creature.params.areaId).world.destroyBody(entityBodies(creature.params.id).b2Body)
        entityBodies = entityBodies - creature.params.id
      case _ =>
    }

    entityBodies.values.foreach(_.update(gameState, this))

  }

  def createContactListener(world: World, physicsQueue: ListBuffer[PhysicsEvent]): Unit = {
    val contactListener: ContactListener = new ContactListener {
      override def beginContact(contact: Contact): Unit = {
        val objA = contact.getFixtureA.getBody.getUserData
        val objB = contact.getFixtureB.getBody.getUserData

        def onContactStart(pair: (AnyRef, AnyRef)): Unit = {
          pair match { // will run onContact twice for same type objects!
            case (entityBody: EntityBody, abilityComponentBody: ComponentBody) =>
              if (entityBody.creatureId != abilityComponentBody.creatureId) {
                physicsQueue.prepend(
                  AbilityComponentCollision(
                    abilityComponentBody.creatureId,
                    abilityComponentBody.abilityId,
                    abilityComponentBody.componentId,
                    entityBody.creatureId
                  )
                )
              }
            case (entityBody: EntityBody, areaGateBody: AreaGateBody) =>
              physicsQueue.prepend(AreaGateCollision(entityBody.creatureId, areaGateBody.areaGate))
            case _ =>
          }
        }

        onContactStart(objA, objB)
        onContactStart(objB, objA)
      }

      override def endContact(contact: Contact): Unit = {
        val objA = contact.getFixtureA.getBody.getUserData
        val objB = contact.getFixtureB.getBody.getUserData

        def onContactEnd(pair: (AnyRef, AnyRef)): Unit = {
          pair match { // will run onContact twice for same type objects!
            case (entityBody: EntityBody, _: AreaGateBody) =>
              physicsQueue.prepend(LeftAreaGateEvent(entityBody.creatureId))
            case _ =>
          }
        }

        onContactEnd(objA, objB)
        onContactEnd(objB, objA)
      }
      override def preSolve(contact: Contact, oldManifold: Manifold): Unit = {}

      override def postSolve(contact: Contact, impulse: ContactImpulse): Unit = {}
    }

    world.setContactListener(contactListener)
  }

  def dispose(): Unit = terrains.values.foreach(_.dispose())
}
