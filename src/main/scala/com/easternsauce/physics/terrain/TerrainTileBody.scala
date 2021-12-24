package com.easternsauce.physics.terrain

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
    body = Terrain.createTileBody(world, x, y, tileWidth, tileHeight)
  }

}
