package com.easternsauce.helper

import com.badlogic.gdx.math.Rectangle
import com.easternsauce.model.GameState
import com.easternsauce.model.item.Item
import com.easternsauce.util.{Constants, Vector2Wrapper}

object LootPickupMenuHelper {
  def menuOptionPosX(i: Int): Float = Constants.WindowWidth / 2 - 100f

  def menuOptionPosY(i: Int): Float = 150f - i * 30f

  def menuOptionRect(i: Int): Rectangle = {
    val x = menuOptionPosX(i)
    val y = menuOptionPosY(i)
    new Rectangle(x - 25f, y - 20f, 300f, 25)
  }

  def lootPickupMenuItems(gameState: GameState): List[(Item, Int)] =
    gameState.lootPilePickupMenu.visibleLootPiles.flatMap {
      case (areaId, lootPileId) => gameState.areas(areaId).params.lootPiles(lootPileId).items
    }.zipWithIndex

  def inLootPickupMenu(gameState: GameState, mousePos: Vector2Wrapper): Boolean = {
    val mousePosX = mousePos.x
    val mousePosY = mousePos.y
    !LootPickupMenuHelper.lootPickupMenuItems(gameState).forall {
      case (_, i) =>
        val rect: Rectangle = LootPickupMenuHelper.menuOptionRect(i)
        !rect.contains(mousePosX, mousePosY)
    }
  }
}
