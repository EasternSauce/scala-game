package com.easternsauce.view.renderer.hud

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.easternsauce.hud.LootPileMenuConfig
import com.easternsauce.model.GameState
import com.easternsauce.system.Assets
import com.easternsauce.util.{RendererBatch, Vector2Wrapper}

case class LootPickupMenuRenderer() {
  import com.easternsauce.system.Assets.bitmapFontToEnrichedBitmapFont

  def render(gameState: GameState, batch: RendererBatch, mousePosition: Vector2Wrapper): Unit = {

    val menuOpen = gameState.lootPilePickupMenuOpen

    if (menuOpen) {

      gameState.lootPilePickupMenu.visibleLootPiles
        .flatMap { case (areaId, lootPileId) => gameState.areas(areaId).params.lootPiles(lootPileId).items }
        .zipWithIndex
        .foreach {
          case (item, i) =>
            val rect: Rectangle = LootPileMenuConfig.menuOptionRect(i)

            batch.shapeDrawer.filledRectangle(rect, Color.DARK_GRAY)
            val text = "> " + item.name + (if (item.quantity > 1) " (" + item.quantity + ")" else "")
            println(i + " " + text)
            Assets.defaultFont.draw(
              batch.spriteBatch,
              text,
              LootPileMenuConfig.menuOptionPosX(i),
              LootPileMenuConfig.menuOptionPosY(i),
              Color.WHITE
            )

            if (rect.contains(mousePosition.x, mousePosition.y)) {
              batch.shapeDrawer.rectangle(rect, Color.RED)
            }
        }
    }
  }
}
