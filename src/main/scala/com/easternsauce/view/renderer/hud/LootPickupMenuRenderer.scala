package com.easternsauce.view.renderer.hud

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.easternsauce.model.GameState
import com.easternsauce.util.{Constants, RendererBatch}

case class LootPickupMenuRenderer() {
  def render(gameState: GameState, batch: RendererBatch): Unit = {
    val menuOpen = gameState.lootPilePickupMenu.menuOpen

    val x = optionPosX(0)
    val y = optionPosY(0)
    val rect = new Rectangle(x - 25f, y - 20f, 300f, 25)
    if (menuOpen) {
      batch.shapeDrawer.filledRectangle(rect, Color.DARK_GRAY)
    }
  }

  private def optionPosX(i: Int): Float = Constants.WindowWidth / 2 - 100f

  private def optionPosY(i: Int): Float = 150f - i * 30f
}
