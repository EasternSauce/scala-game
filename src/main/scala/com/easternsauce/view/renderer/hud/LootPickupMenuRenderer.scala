package com.easternsauce.view.renderer.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.easternsauce.model.GameState
import com.easternsauce.model.area.loot.LootPile
import com.easternsauce.model.item.Item
import com.easternsauce.system.Assets
import com.easternsauce.util.{Constants, RendererBatch}
import com.softwaremill.quicklens._

import scala.collection.mutable.ListBuffer

case class LootPickupMenuRenderer() {
  import com.easternsauce.system.Assets.bitmapFontToEnrichedBitmapFont

  def render(gameState: GameState, batch: RendererBatch): Unit = {
    val mouseX = Gdx.input.getX
    val mouseY = Gdx.input.getY

    val menuOpen = gameState.lootPilePickupMenuOpen

    val x = menuOptionPosX(0)
    val y = menuOptionPosY(0)
    val rect = new Rectangle(x - 25f, y - 20f, 300f, 25)
    if (menuOpen) {
      batch.shapeDrawer.filledRectangle(rect, Color.DARK_GRAY)

      var i = 0 // TODO: how to avoid using var?
      for {
        (areaId, lootPileId) <- gameState.lootPilePickupMenu.visibleLootPiles
        item <- gameState.areas(areaId).params.lootPiles(lootPileId).items
      } {

        val rect: Rectangle = menuOptionRect(i)

        batch.shapeDrawer.filledRectangle(rect, Color.DARK_GRAY)
        val text = "> " + item.name + (if (item.quantity > 1) " (" + item.quantity + ")" else "")
        Assets.defaultFont.draw(batch.spriteBatch, text, menuOptionPosX(i), menuOptionPosY(i), Color.WHITE)

        if (rect.contains(mouseX, mouseY)) batch.shapeDrawer.rectangle(rect, Color.RED)

        i += 1
      }
    }
  }

  def pickUpItemClick(gameState: GameState): Unit = {
    val mouseX = Gdx.input.getX
    val mouseY = Gdx.input.getY

    val itemOptions = gameState.lootPilePickupMenu.visibleLootPiles
      .map {
        case (areaId, lootPileId) => (areaId, lootPileId, gameState.areas(areaId).params.lootPiles(lootPileId).items)
      }
      .foldLeft(List[(String, String, Item)]()) {
        case (acc, (areaId, lootPileId, items)) => acc ++ items.map(item => (areaId, lootPileId, item))
      }

    val scheduledToRemove: ListBuffer[((String, String, Item), LootPile)] = ListBuffer()

    for {
      (areaId, lootPileId, item) <- itemOptions
    } {
      val i = itemOptions.indexOf((areaId, lootPileId, item))

      val rect: Rectangle = menuOptionRect(i)

      if (rect.contains(mouseX, mouseY)) {
        val success = gameState.player.tryPickUpItem(item)
        if (success) {
          val lootPile = gameState.areas(areaId).params.lootPiles(lootPileId)
//          val lootPile: LootPile = item.lootPile.get // TODO: lootPile back reference?
//          if (
//            lootPile.isTreasure && !treasureLootedList.contains(
//              areaMap(currentAreaId.get).id -> lootPile.treasureId.get
//            )
//          )
//            treasureLootedList += (areaMap(currentAreaId.get).id -> lootPile.treasureId.get)
          scheduledToRemove += ((areaId, lootPileId, item) -> lootPile)
        }
      }
    }

    scheduledToRemove.foreach {
      case ((areaId, lootPileId, item), lootPile) =>
//        Assets.sound(Assets.coinBagSound).play(0.3f) TODO

        if (lootPile.items.size == 1) {
//          val world = lootPile.b2Body.getWorld TODO
//          world.destroyBody(lootPile.b2Body)

          val stuff = gameState.modify(_.areas.at(areaId).params.lootPiles).using(_.filterNot(Set(lootPile)))
//          lootPile.area.lootPileList -= lootPile TODO
        }
        gameState.modify(_.areas.at(areaId).params.lootPiles.at(lootPileId).items).using(_.filterNot(Set(item)))
//        lootPile.items -= item
//        item.lootPile = None
    }
  }

  private def menuOptionRect(i: Int): Rectangle = {
    val x = menuOptionPosX(i)
    val y = menuOptionPosY(i)
    new Rectangle(x - 25f, y - 20f, 300f, 25)
  }

  private def menuOptionPosX(i: Int): Float = Constants.WindowWidth / 2 - 100f

  private def menuOptionPosY(i: Int): Float = 150f - i * 30f
}
