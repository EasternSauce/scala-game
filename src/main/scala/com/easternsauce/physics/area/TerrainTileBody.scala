package com.easternsauce.physics.area

import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.physics.box2d._

case class TerrainTileBody(
  layer: Int,
  x: Int,
  y: Int,
  tileWidth: Float,
  tileHeight: Float,
  flyover: Boolean,
  polygon: Polygon
) {
  var body: Body = _

  def init(world: World): Unit = {
    val bodyDef = new BodyDef()
    bodyDef.`type` = BodyDef.BodyType.StaticBody
    bodyDef.position
      .set(x * tileWidth + tileWidth / 2, y * tileHeight + tileHeight / 2)

    body = world.createBody(bodyDef)

    body.setUserData(this)

    val shape: PolygonShape = new PolygonShape()

    shape.setAsBox(tileWidth / 2, tileHeight / 2)

    val fixtureDef: FixtureDef = new FixtureDef

    fixtureDef.shape = shape

    body.createFixture(fixtureDef)
  }
}
