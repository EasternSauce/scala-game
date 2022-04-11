package com.easternsauce.view.physics.entity

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d._
import com.easternsauce.model.GameState
import com.easternsauce.model.event.CreatureDeathEvent
import com.easternsauce.view.physics.terrain.Terrain
import com.easternsauce.view.physics.{B2BodyFactory, PhysicsController}

case class EntityBody(creatureId: String) {

  var b2Body: Body = _

  var componentBodies: Map[(String, String), ComponentBody] = Map()

  var currentAreaId: String = _

  def init(gameState: GameState, physicsController: PhysicsController, areaId: String): Unit = {
    val creature = gameState.creatures(creatureId)

    val terrain: Terrain = physicsController.terrain(areaId)

    b2Body = B2BodyFactory.createCreatureB2body(world = terrain.world, entityBody = this, creature = creature)

    componentBodies = creature.params.abilities.flatMap {
      case abilityId -> _ =>
        val components = gameState.abilities(creatureId, abilityId).components

        components.keys.map(
          componentId => (abilityId, componentId) -> ComponentBody(creatureId, abilityId, componentId)
        )
    }

    currentAreaId = areaId
  }

  def update(gameState: GameState, physicsController: PhysicsController): Unit = {
    if (gameState.events.contains(CreatureDeathEvent(creatureId))) {
      b2Body.getFixtureList.get(0).setSensor(true)
    }

    val ableToMove = !gameState.creatures(creatureId).isEffectActive("stagger")

    val bodyCreated = physicsController.entityBodies.contains(creatureId)

    val vectorX = gameState.creatures(creatureId).params.velocity.x
    val vectorY = gameState.creatures(creatureId).params.velocity.y

    if (bodyCreated && ableToMove)
      physicsController.entityBodies(creatureId).setVelocity(new Vector2(vectorX, vectorY))

    componentBodies.values.foreach(_.update(gameState, physicsController, currentAreaId))
  }

  def pos: Vector2 = b2Body.getWorldCenter

  def setVelocity(velocity: Vector2): Unit = b2Body.setLinearVelocity(velocity)

}
