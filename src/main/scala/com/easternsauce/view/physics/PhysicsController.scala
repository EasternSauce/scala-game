package com.easternsauce.view.physics

import com.badlogic.gdx.physics.box2d._
import com.easternsauce.event._
import com.easternsauce.model.GameState
import com.easternsauce.model.event._
import com.easternsauce.view.physics.entity.{ComponentBody, EntityBody}
import com.easternsauce.view.physics.terrain.{AreaGateBody, LootPileBody, Terrain}

import scala.collection.mutable.ListBuffer

case class PhysicsController(terrains: Map[String, Terrain], areaGates: List[AreaGateBody]) {
  var entityBodies: Map[String, EntityBody] = Map()
  var lootPileBodies: Map[(String, String), LootPileBody] = Map()

  def init(gameState: GameState, physicsEventQueue: ListBuffer[PhysicsEvent]): Unit = {

    terrains.values.foreach(terrain => {
      terrain.init()
      createContactListener(terrain.world, physicsEventQueue)
    })

    entityBodies = gameState.creatures.keys.map(creatureId => creatureId -> EntityBody(creatureId)).toMap

    entityBodies.values.foreach(entityBody => {

      val areaId = gameState.creatures(entityBody.creatureId).params.areaId

      entityBody.init(gameState = gameState, physicsController = this, areaId = areaId)
    })

    val areaLootPileCombinations: List[(String, String)] = gameState.areas.toList.foldLeft(List[(String, String)]()) {
      case (acc, (k, v)) => acc ++ List().zipAll(v.params.lootPiles.keys.toList, k, "")
    }

    lootPileBodies = areaLootPileCombinations.map {
      case (areaId, lootPileId) => (areaId, lootPileId) -> LootPileBody(areaId, lootPileId)
    }.toMap

    lootPileBodies.values.foreach(_.init(terrains, gameState))

  }

  def update(gameState: GameState): Unit = {
    gameState.events.foreach {
      case UpdatePhysicsOnAreaChangeEvent(creatureId, oldAreaId, newAreaId, _, _) =>
        terrains(oldAreaId).world.destroyBody(entityBodies(creatureId).b2Body)
        entityBodies(creatureId).init(gameState = gameState, physicsController = this, areaId = newAreaId)
      case UpdatePhysicsOnEnemySpawnEvent(creatureId, areaId) =>
        entityBodies = entityBodies + (creatureId -> {
          val entityBody = EntityBody(creatureId)
          entityBody.init(gameState = gameState, physicsController = this, areaId = areaId)
          entityBody
        })
      case UpdatePhysicsOnEnemyDespawnEvent(creatureId, areaId) =>
        val world = terrains(areaId).world
        world.destroyBody(entityBodies(creatureId).b2Body)
        entityBodies(creatureId).componentBodies.foreach {
          case (_, componentBody) =>
            if (componentBody.b2Body != null && componentBody.b2Body.isActive) world.destroyBody(componentBody.b2Body)
        }
        entityBodies = entityBodies - creatureId
      case UpdatePhysicsOnLootPileSpawnEvent(areaId, lootPileId) =>
        lootPileBodies = lootPileBodies + ((areaId, lootPileId) -> {
          val lootPileBody = LootPileBody(areaId, lootPileId)
          lootPileBody.init(terrains, gameState)
          lootPileBody
        })
      case UpdatePhysicsOnLootPileDespawnEvent(areaId, lootPileId) =>
        val world = terrains(areaId).world
        world.destroyBody(lootPileBodies(areaId, lootPileId).b2Body)

      case _ =>
    }

    entityBodies.values.foreach(_.update(gameState, this))

  }

  def createContactListener(world: World, physicsEventQueue: ListBuffer[PhysicsEvent]): Unit = {
    val contactListener: ContactListener = new ContactListener {
      override def beginContact(contact: Contact): Unit = {
        val objA = contact.getFixtureA.getBody.getUserData
        val objB = contact.getFixtureB.getBody.getUserData

        def onContactStart(pair: (AnyRef, AnyRef)): Unit = {
          pair match { // will run onContact twice for same type objects!
            case (entityBody: EntityBody, abilityComponentBody: ComponentBody) =>
              if (entityBody.creatureId != abilityComponentBody.creatureId) {
                physicsEventQueue.prepend(
                  AbilityComponentCollisionEvent(
                    abilityComponentBody.creatureId,
                    abilityComponentBody.abilityId,
                    abilityComponentBody.componentId,
                    entityBody.creatureId
                  )
                )
              }
            case (entityBody: EntityBody, areaGateBody: AreaGateBody) =>
              physicsEventQueue.prepend(AreaGateCollisionStartEvent(entityBody.creatureId, areaGateBody))
            case (entityBody: EntityBody, lootPileBody: LootPileBody) =>
              physicsEventQueue.prepend(
                LootPileCollisionStartEvent(entityBody.creatureId, lootPileBody.areaId, lootPileBody.lootPileId)
              )
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
              physicsEventQueue.prepend(AreaGateCollisionEndEvent(entityBody.creatureId))
            case (entityBody: EntityBody, lootPileBody: LootPileBody) =>
              physicsEventQueue.prepend(
                LootPileCollisionEndEvent(entityBody.creatureId, lootPileBody.areaId, lootPileBody.lootPileId)
              )
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
