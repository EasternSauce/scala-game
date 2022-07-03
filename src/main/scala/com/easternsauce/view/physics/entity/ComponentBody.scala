package com.easternsauce.view.physics.entity

import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.{Body, World}
import com.easternsauce.model.GameState
import com.easternsauce.model.creature.ability.ComponentType
import com.easternsauce.model.event.{UpdatePhysicsOnComponentCreateBodyEvent, UpdatePhysicsOnComponentDestroyBodyEvent}
import com.easternsauce.view.physics.terrain.Terrain
import com.easternsauce.view.physics.{B2BodyFactory, PhysicsController}

case class ComponentBody(creatureId: String, abilityId: String, componentId: String) {
  var b2Body: Body = _
  var world: World = _
  private val sprite = new Sprite()
  var isActive = false

  def hitboxVertices(gameState: GameState): Array[Float] = {
    val ability = gameState.abilities(creatureId, abilityId)

    sprite.setSize(
      ability.components(componentId).params.abilityHitbox.width,
      ability.components(componentId).params.abilityHitbox.height
    )
    sprite.setCenter(0, 0)
    sprite.setOriginCenter()
    sprite.setRotation(ability.components(componentId).params.abilityHitbox.rotationAngle)
    sprite.setScale(ability.components(componentId).params.abilityHitbox.scale)

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
    val component = ability.components(componentId)

    val vertices = hitboxVertices(gameState)

    b2Body = B2BodyFactory.createAbilityComponentB2body(
      world = world,
      abilityBody = this,
      posX = component.params.abilityHitbox.x,
      posY = component.params.abilityHitbox.y,
      vertices = vertices
    )

  }

  def update(gameState: GameState, physicsController: PhysicsController, areaId: String): Unit = {
    val ability = gameState.abilities(creatureId, abilityId)
    val component = ability.components(componentId)

    val terrain: Terrain = physicsController.terrains(areaId)

    if (gameState.events.contains(UpdatePhysicsOnComponentCreateBodyEvent(creatureId, abilityId, componentId))) {
      init(terrain.world, gameState)
      isActive = true
    }

    if (
      isActive && gameState.events.contains(
        UpdatePhysicsOnComponentDestroyBodyEvent(creatureId, abilityId, componentId)
      )
    ) {
      destroy()
      isActive = false
    }

    if (isActive) {
      if (component.specification.componentType == ComponentType.MeleeAttack) {
        b2Body.setTransform(component.params.abilityHitbox.x, component.params.abilityHitbox.y, 0f)
      } else if (component.specification.componentType == ComponentType.RangedProjectile) {
        b2Body.setLinearVelocity(
          component.params.dirVector.x * component.speed,
          component.params.dirVector.y * component.speed
        )
      }

    }

  }

  def pos: Vector2 = b2Body.getWorldCenter

  def destroy(): Unit = {
    world.destroyBody(b2Body)
    b2Body = null
  }

}
