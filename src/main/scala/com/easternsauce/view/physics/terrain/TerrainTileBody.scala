package com.easternsauce.view.physics.terrain

import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.physics.box2d._
import com.easternsauce.view.physics.B2BodyFactory

case class TerrainTileBody(
  x: Int,
  y: Int,
  tileWidth: Float,
  tileHeight: Float,
  layer: Int = 0,
  flyover: Boolean = false
) {
  var b2Body: Body = _
  val polygon: Polygon = new Polygon(
    Array(
      x * tileWidth,
      y * tileHeight,
      x * tileWidth + tileWidth,
      y * tileHeight,
      x * tileWidth + tileWidth,
      y * tileHeight + tileHeight,
      x * tileWidth,
      y * tileHeight + tileHeight
    )
  )

  def init(world: World): Unit = {
    b2Body = B2BodyFactory.createTerrainTileB2body(world = world, terrainTileBody = this)
  }

}
