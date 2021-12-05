package com.easternsauce.physics.creature

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d._
import com.easternsauce.model.GameState
import com.easternsauce.physics.PhysicsController
import com.easternsauce.physics.terrain.Terrain

case class EntityBody(id: String) {

  var body: Body = _

  def init(gameState: GameState, physicsController: PhysicsController, areaId: String): Unit = {
    val creature = gameState.creatures(id)

    val terrain: Terrain = physicsController.terrain(areaId)

    val bodyDef = new BodyDef()
    bodyDef.position.set(creature.params.posX, creature.params.posY)

    bodyDef.`type` = BodyDef.BodyType.DynamicBody
    val b2Body = terrain.world.createBody(bodyDef)
    b2Body.setUserData(this)
    b2Body.setSleepingAllowed(false)

    val fixtureDef: FixtureDef = new FixtureDef()
    val shape: CircleShape = new CircleShape()
    shape.setRadius(creature.width / 2)

    fixtureDef.shape = shape
    fixtureDef.isSensor = false
    b2Body.createFixture(fixtureDef)
    b2Body.setLinearDamping(10f)

    body = b2Body
  }

  def pos: Vector2 = body.getWorldCenter

  def setVelocity(velocity: Vector2): Unit = body.setLinearVelocity(velocity)
}
