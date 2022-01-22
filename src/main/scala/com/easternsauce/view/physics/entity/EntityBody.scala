package com.easternsauce.view.physics.entity

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d._
import com.easternsauce.view.physics.terrain.Terrain
import com.easternsauce.view.physics.{B2BodyFactory, PhysicsController}
import com.easternsauce.model.GameState
import com.easternsauce.model.event.CreatureDeathEvent

case class EntityBody(creatureId: String) {

  var b2Body: Body = _

  var abilityComponentBodies: Map[(String, String), AbilityComponentBody] = Map()

  var currentAreaId: String = _

  def init(gameState: GameState, physicsController: PhysicsController, areaId: String): Unit = {
    val creature = gameState.creatures(creatureId)

    val terrain: Terrain = physicsController.terrain(areaId)

    b2Body = B2BodyFactory.createCreatureB2body(world = terrain.world, entityBody = this, creature = creature)

    abilityComponentBodies = creature.params.abilities.flatMap {
      case abilityId -> _ =>
        val components = gameState.abilities(creatureId, abilityId).components

        components.keys.map(
          componentId => (abilityId, componentId) -> AbilityComponentBody(creatureId, abilityId, componentId)
        )
    }

    currentAreaId = areaId
  }

  def update(gameState: GameState, physicsController: PhysicsController): Unit = {
    if (gameState.events.contains(CreatureDeathEvent(creatureId))) {
      b2Body.getFixtureList.get(0).setSensor(true)
    }

    abilityComponentBodies.values.foreach(_.update(gameState, physicsController, currentAreaId))
  }

  def pos: Vector2 = b2Body.getWorldCenter

  def setVelocity(velocity: Vector2): Unit = b2Body.setLinearVelocity(velocity)

}
