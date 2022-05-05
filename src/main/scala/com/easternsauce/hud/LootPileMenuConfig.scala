package com.easternsauce.hud

import com.badlogic.gdx.math.Rectangle
import com.easternsauce.util.Constants

object LootPileMenuConfig {
  def menuOptionPosX(i: Int): Float = Constants.WindowWidth / 2 - 100f

  def menuOptionPosY(i: Int): Float = 150f - i * 30f

  def menuOptionRect(i: Int): Rectangle = {
    val x = menuOptionPosX(i)
    val y = menuOptionPosY(i)
    new Rectangle(x - 25f, y - 20f, 300f, 25)
  }
}
