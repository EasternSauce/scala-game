package com.easternsauce.view.renderer.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.easternsauce.hud.LootPileMenuConfig
import com.easternsauce.model.GameState
import com.easternsauce.system.Assets
import com.easternsauce.util.RendererBatch

case class LootPickupMenuRenderer() {
  import com.easternsauce.system.Assets.bitmapFontToEnrichedBitmapFont

  def render(gameState: GameState, batch: RendererBatch): Unit = {
    val mouseX = Gdx.input.getX
    val mouseY = Gdx.input.getY

    val menuOpen = gameState.lootPilePickupMenuOpen

    val x = LootPileMenuConfig.menuOptionPosX(0)
    val y = LootPileMenuConfig.menuOptionPosY(0)

    if (menuOpen) {

      var i = 0 // TODO: how to avoid using var?
      for {
        (areaId, lootPileId) <- gameState.lootPilePickupMenu.visibleLootPiles
        item <- gameState.areas(areaId).params.lootPiles(lootPileId).items
      } {

        val rect: Rectangle = LootPileMenuConfig.menuOptionRect(i)

        batch.shapeDrawer.filledRectangle(rect, Color.DARK_GRAY)
        val text = "> " + item.name + (if (item.quantity > 1) " (" + item.quantity + ")" else "")
        Assets.defaultFont.draw(
          batch.spriteBatch,
          text,
          LootPileMenuConfig.menuOptionPosX(i),
          LootPileMenuConfig.menuOptionPosY(i),
          Color.WHITE
        )

        if (rect.contains(mouseX, mouseY)) batch.shapeDrawer.rectangle(rect, Color.RED)

        i += 1
      }
    }
  }
}
