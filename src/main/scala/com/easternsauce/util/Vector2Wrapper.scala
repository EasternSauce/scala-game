package com.easternsauce.util

import com.badlogic.gdx.math.Vector2

case class Vector2Wrapper(x: Float, y: Float) {

  val vector2: Vector2 = new Vector2(x, y)

  def normal: Vector2Wrapper = {
    val v2 = vector2.cpy().nor()
    Vector2Wrapper(v2.x, v2.y)
  }

  def rotate(degrees: Float): Vector2Wrapper = {
    val v2 = vector2.cpy().rotateDeg(degrees)
    Vector2Wrapper(v2.x, v2.y)
  }

  def angleDeg(): Float = {
    vector2.angleDeg()
  }
}
