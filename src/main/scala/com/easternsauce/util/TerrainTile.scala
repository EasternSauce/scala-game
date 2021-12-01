package com.easternsauce.util

import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.physics.box2d.Body

case class TerrainTile(pos: (Int, Int, Int), body: Body, flyover: Boolean, polygon: Polygon)
