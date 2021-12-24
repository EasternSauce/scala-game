package com.easternsauce.box2d_physics.creature

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d._
import com.easternsauce.box2d_physics.terrain.Terrain
import com.easternsauce.box2d_physics.{B2BodyFactory, PhysicsController}
import com.easternsauce.model.GameState

case class EntityBody(id: String) {

  var b2Body: Body = _

  def init(gameState: GameState, physicsController: PhysicsController, areaId: String): Unit = {
    val creature = gameState.creatures(id)

    val terrain: Terrain = physicsController.terrain(areaId)

    b2Body = B2BodyFactory.createCreatureB2body(terrain.world, this, creature)
  }

  def pos: Vector2 = b2Body.getWorldCenter

  def setVelocity(velocity: Vector2): Unit = b2Body.setLinearVelocity(velocity)
}
