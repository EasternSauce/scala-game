package com.easternsauce.box2d_physics.entity

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d._
import com.easternsauce.box2d_physics.terrain.Terrain
import com.easternsauce.box2d_physics.{B2BodyFactory, PhysicsController}
import com.easternsauce.model.GameState
import com.easternsauce.model.events.CreatureDeathEvent

case class EntityBody(creatureId: String) {

  var b2Body: Body = _

  var abilityBodies: Map[String, AbilityBody] = Map()

  var currentAreaId: String = _

  def init(gameState: GameState, physicsController: PhysicsController, areaId: String): Unit = {
    val creature = gameState.creatures(creatureId)

    val terrain: Terrain = physicsController.terrain(areaId)

    b2Body = B2BodyFactory.createCreatureB2body(world = terrain.world, entityBody = this, creature = creature)

    abilityBodies = creature.params.abilities.map {
      case abilityId -> _ => abilityId -> AbilityBody(creatureId, abilityId)
    }

    currentAreaId = areaId
  }

  def update(gameState: GameState, physicsController: PhysicsController): Unit = {
    if (gameState.events.contains(CreatureDeathEvent(creatureId))) {
      b2Body.getFixtureList.get(0).setSensor(true)
    }

    abilityBodies.values.foreach(_.update(gameState, physicsController, currentAreaId))
  }

  def pos: Vector2 = b2Body.getWorldCenter

  def setVelocity(velocity: Vector2): Unit = b2Body.setLinearVelocity(velocity)

}
