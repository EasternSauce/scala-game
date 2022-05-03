package com.easternsauce.view.physics.terrain

import com.badlogic.gdx.physics.box2d.Body
import com.easternsauce.view.physics.B2BodyFactory

case class AreaGateBody(area1Id: String, x1: Float, y1: Float, area2Id: String, x2: Float, y2: Float) {

  val width = 1.5f
  val height = 1.5f

  private var entryPoint1Body: Body = _
  private var entryPoint2Body: Body = _

  def init(terrains: Map[String, Terrain]): Unit = {
    entryPoint1Body = initEntryPointBody(terrains, area1Id, x1, y1, width, height)
    entryPoint2Body = initEntryPointBody(terrains, area2Id, x2, y2, width, height)
  }

  def destroy(): Unit = {
    entryPoint1Body.getWorld.destroyBody(entryPoint1Body)
    entryPoint2Body.getWorld.destroyBody(entryPoint2Body)
  }

  def initEntryPointBody(
    terrains: Map[String, Terrain],
    areaId: String,
    x: Float,
    y: Float,
    width: Float,
    height: Float
  ): Body = {

    val world = terrains(areaId).world
    B2BodyFactory.createAreaGateEntryPointB2body(
      world = world,
      areaGateBody = this,
      posX = x,
      posY = y,
      width = width,
      height = height
    )
  }

}
