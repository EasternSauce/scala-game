package com.easternsauce.view.physics.terrain

import com.badlogic.gdx.physics.box2d.{Body, World}
import com.easternsauce.view.physics.B2BodyFactory

case class AreaGateBody(areaGate: AreaGatePair, x: Float, y: Float, width: Float, height: Float) {
  var b2Body: Body = _

  def init(world: World): Unit = {
    b2Body = B2BodyFactory.createAreaGateB2body(
      world = world,
      areaGateBody = this,
      posX = x,
      posY = y,
      width = width,
      height = height
    )
  }
}
