package com.easternsauce.box2d_physics.entity

import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.physics.box2d.{Body, World}
import com.easternsauce.box2d_physics.terrain.Terrain
import com.easternsauce.box2d_physics.{B2BodyFactory, PhysicsController}
import com.easternsauce.model.GameState
import com.easternsauce.model.events.{AbilityCreateBodyEvent, AbilityDestroyBodyEvent}

case class AbilityBody(creatureId: String, abilityId: String) {
  var b2Body: Body = _
  var world: World = _
  private val sprite = new Sprite()
  private var isActive = false

  def hitboxVertices(gameState: GameState): Array[Float] = {
    val ability = gameState.abilities(creatureId, abilityId)

    sprite.setSize(ability.params.abilityHitbox.width, ability.params.abilityHitbox.height)
    sprite.setCenter(0, 0)
    sprite.setOriginCenter()
    sprite.setRotation(ability.params.abilityHitbox.rotationAngle)
    sprite.setScale(ability.params.abilityHitbox.scale)

    val vertices = sprite.getVertices
    Array(
      vertices(0), // take only coordinate sprite vertices
      vertices(1),
      vertices(5),
      vertices(6),
      vertices(10),
      vertices(11),
      vertices(15),
      vertices(16)
    )
  }

  def init(world: World, gameState: GameState): Unit = {
    this.world = world

    val ability = gameState.abilities(creatureId, abilityId)

    val vertices = hitboxVertices(gameState)

    b2Body = B2BodyFactory.createAbilityB2body(
      world = world,
      abilityBody = this,
      posX = ability.params.abilityHitbox.x,
      posY = ability.params.abilityHitbox.y,
      vertices = vertices
    )

  }

  def update(gameState: GameState, physicsController: PhysicsController, areaId: String): Unit = {
    val ability = gameState.abilities(creatureId, abilityId)

    val terrain: Terrain = physicsController.terrain(areaId)

    if (gameState.events.contains(AbilityCreateBodyEvent(creatureId, abilityId))) {
      init(terrain.world, gameState)
      isActive = true
    }

    if (gameState.events.contains(AbilityDestroyBodyEvent(creatureId, abilityId))) {
      destroy()
      isActive = false
    }

    if (isActive) {
      b2Body.setTransform(ability.params.abilityHitbox.x, ability.params.abilityHitbox.y, 0f)
    }
  }

  def destroy(): Unit = {
    world.destroyBody(b2Body)
  }

}
