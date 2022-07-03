package com.easternsauce.view.physics.entity

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d._
import com.easternsauce.model.GameState
import com.easternsauce.model.creature.ability.AbilityState
import com.easternsauce.model.event.UpdatePhysicsOnCreatureDeathEvent
import com.easternsauce.view.physics.terrain.Terrain
import com.easternsauce.view.physics.{B2BodyFactory, PhysicsController}

case class EntityBody(creatureId: String) {

  var b2Body: Body = _

  var componentBodies: Map[(String, String), ComponentBody] = Map()

  var currentAreaId: String = _

  def init(gameState: GameState, physicsController: PhysicsController, areaId: String): Unit = {
    val creature = gameState.creatures(creatureId)

    val terrain: Terrain = physicsController.terrains(areaId)

    b2Body = B2BodyFactory.createCreatureB2body(world = terrain.world, entityBody = this, creature = creature)

    componentBodies = creature.params.abilities.flatMap {
      case abilityId -> _ =>
        val components = gameState.abilities(creatureId, abilityId).components

        components.keys.map(
          componentId =>
            (abilityId, componentId) -> {
              val component = components(componentId)
              val componentBody = ComponentBody(creatureId, abilityId, componentId)
              if (component.params.state == AbilityState.Active) {
                componentBody.init(terrain.world, gameState)
                componentBody.isActive = true
              }
              componentBody
            }
        )
    }

    if (!creature.isAlive) b2Body.getFixtureList.get(0).setSensor(true)

    currentAreaId = areaId
  }

  def update(gameState: GameState, physicsController: PhysicsController): Unit = {
    if (gameState.events.contains(UpdatePhysicsOnCreatureDeathEvent(creatureId))) {
      b2Body.getFixtureList.get(0).setSensor(true)
    }

    val creature = gameState.creatures(creatureId)

    val ableToMove = !creature.isEffectActive("stagger") && !creature.isEffectActive("knockback") && creature.isAlive

    val bodyCreated = physicsController.entityBodies.contains(creatureId)

    val v = creature.params.currentSpeed
    val normalMovingDir = creature.params.movingDir.normal
    val vectorX = normalMovingDir.x * v
    val vectorY = normalMovingDir.y * v

    if (bodyCreated) {
      if (creature.isEffectActive("knockback")) {
        physicsController
          .entityBodies(creatureId)
          .setVelocity(
            new Vector2(
              creature.params.knockbackDir.x * creature.params.knockbackVelocity,
              creature.params.knockbackDir.y * creature.params.knockbackVelocity
            )
          )
      } else if (ableToMove)
        physicsController.entityBodies(creatureId).setVelocity(new Vector2(vectorX, vectorY))
    }

    componentBodies.values.foreach(_.update(gameState, physicsController, currentAreaId))
  }

  def pos: Vector2 = b2Body.getWorldCenter

  def setVelocity(velocity: Vector2): Unit = b2Body.setLinearVelocity(velocity)

}
