package com.easternsauce.view.renderer.hud

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.easternsauce.helper.LootPickupMenuHelper
import com.easternsauce.model.GameState
import com.easternsauce.system.Assets
import com.easternsauce.util.{RendererBatch, Vector2Wrapper}

case class LootPickupMenuRenderer() {
  import com.easternsauce.system.Assets.bitmapFontToEnrichedBitmapFont

  def render(gameState: GameState, batch: RendererBatch, mousePosition: Vector2Wrapper): Unit = {

    if (gameState.lootPilePickupMenu.isOpen && !gameState.inventoryWindow.isOpen) {

      LootPickupMenuHelper
        .lootPickupMenuItems(gameState)
        .foreach {
          case (item, i) =>
            val rect: Rectangle = LootPickupMenuHelper.menuOptionRect(i)

            batch.shapeDrawer.filledRectangle(rect, Color.DARK_GRAY)
            val text = "> " + item.name + (if (item.quantity > 1) " (" + item.quantity + ")" else "")
            Assets.defaultFont.draw(
              batch.spriteBatch,
              text,
              LootPickupMenuHelper.menuOptionPosX(i),
              LootPickupMenuHelper.menuOptionPosY(i),
              Color.WHITE
            )

            if (rect.contains(mousePosition.x, mousePosition.y)) {
              batch.shapeDrawer.rectangle(rect, Color.RED)
            }
        }
    }
  }
}
