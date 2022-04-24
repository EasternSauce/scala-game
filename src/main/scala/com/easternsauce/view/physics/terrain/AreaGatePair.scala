package com.easternsauce.view.physics.terrain

case class AreaGatePair(
  areaFrom: String,
  fromPosX: Float,
  fromPosY: Float,
  areaTo: String,
  toPosX: Float,
  toPosY: Float
) {

  val width = 1.5f
  val height = 1.5f

  private val bodyFrom: AreaGateBody = AreaGateBody(this, fromPosX, fromPosY, width, height)
  private val bodyTo: AreaGateBody = AreaGateBody(this, toPosX, toPosY, width, height)

  def init(terrains: Map[String, Terrain]): Unit = {
    bodyFrom.init(terrains(areaFrom).world)
    bodyTo.init(terrains(areaTo).world)

  }

  def destroy(): Unit = {
    bodyFrom.b2Body.getWorld.destroyBody(bodyFrom.b2Body)
    bodyTo.b2Body.getWorld.destroyBody(bodyTo.b2Body)
  }

}
