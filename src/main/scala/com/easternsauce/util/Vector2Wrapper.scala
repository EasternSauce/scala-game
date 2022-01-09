package com.easternsauce.util

import com.badlogic.gdx.math.Vector2

case class Vector2Wrapper(x: Float, y: Float) {

  var vector2: Vector2 = new Vector2(x, y)

  def normal: Vector2Wrapper = {
    val v2 = vector2.nor()
    Vector2Wrapper(v2.x, v2.y)
  }
}
