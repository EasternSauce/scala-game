package com.easternsauce.view.renderer.hud

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.easternsauce.hud.InventoryData
import com.easternsauce.model.GameState
import com.easternsauce.system.Assets
import com.easternsauce.util.{InventoryMapping, RendererBatch, Vector2Wrapper}

case class InventoryRenderer() {
  import com.easternsauce.system.Assets.bitmapFontToEnrichedBitmapFont

  val backgroundImage = new Image(Assets.atlas.findRegion("background2"))

  val icons: Array[Array[TextureRegion]] = Assets.atlas.findRegion("nice_icons").split(32, 32)

  backgroundImage.setBounds(
    InventoryData.backgroundOuterRect.x,
    InventoryData.backgroundOuterRect.y,
    InventoryData.backgroundOuterRect.width,
    InventoryData.backgroundOuterRect.height
  )

  def render(gameState: GameState, batch: RendererBatch, mousePosition: Vector2Wrapper): Unit = {
    if (gameState.inventoryWindow.inventoryOpen) {
      backgroundImage.draw(batch.spriteBatch, 1.0f)

      InventoryData.inventoryRectangles.values.foreach(rect => {
        batch.shapeDrawer.filledRectangle(rect.x - 3, rect.y - 3, rect.width + 6, rect.height + 6, Color.BROWN)
        batch.shapeDrawer.filledRectangle(rect, Color.BLACK)
      })

      InventoryData.equipmentRectangles.foreach {
        case (index, rect) =>
          batch.shapeDrawer.filledRectangle(rect.x - 3, rect.y - 3, rect.width + 6, rect.height + 6, Color.BROWN)
          batch.shapeDrawer.filledRectangle(rect, Color.BLACK)

          Assets.defaultFont.draw(
            batch.spriteBatch,
            InventoryMapping.equipmentTypeNames(index) + ":",
            rect.x - InventoryData.slotSize / 2 - 170,
            rect.y + InventoryData.slotSize / 2 + 7,
            Color.DARK_GRAY
          )
      }

      renderPlayerItems(gameState, batch, mousePosition)
      renderDescription(gameState, batch, mousePosition)
    }

  }

  def renderPlayerItems(gameState: GameState, batch: RendererBatch, mousePosition: Vector2Wrapper): Unit = {
    val player = gameState.player

    val inventory = player.params.inventoryItems
    val equipment = player.params.equipmentItems

    inventory
      .filter {
        case (index, _) =>
          if (gameState.inventoryWindow.inventoryItemBeingMoved.nonEmpty)
            gameState.inventoryWindow.inventoryItemBeingMoved.get != index
          else true
      }
      .foreach {
        case (index, item) =>
          val (iconPosX, iconPosY) = item.template.iconPosition
          val textureRegion = icons(iconPosX)(iconPosY)
          val x = InventoryData.inventorySlotPositionX(index)
          val y = InventoryData.inventorySlotPositionY(index)
          batch.spriteBatch.draw(textureRegion, x, y, InventoryData.slotSize, InventoryData.slotSize)

          if (item.quantity > 1) {
            Assets.defaultFont.draw(batch.spriteBatch, item.quantity.toString, x, y + 15, Color.WHITE)
          }
      }

    equipment
      .filter {
        case (index, _) =>
          if (gameState.inventoryWindow.equipmentItemBeingMoved.nonEmpty)
            gameState.inventoryWindow.equipmentItemBeingMoved.get != index
          else true
      }
      .foreach {
        case (index, item) =>
          val (iconPosX, iconPosY) = item.template.iconPosition
          val textureRegion = icons(iconPosX)(iconPosY)
          val x = InventoryData.equipmentSlotPositionX(index)
          val y = InventoryData.equipmentSlotPositionY(index)
          batch.spriteBatch.draw(textureRegion, x, y, InventoryData.slotSize, InventoryData.slotSize)

          if (item.quantity > 1) {
            Assets.defaultFont.draw(batch.spriteBatch, item.quantity.toString, x, y + 15, Color.WHITE)
          }
      }

    // render moved item on cursor

    if (gameState.inventoryWindow.inventoryItemBeingMoved.nonEmpty) {
      val (iconPosX, iconPosY) = inventory(gameState.inventoryWindow.inventoryItemBeingMoved.get).template.iconPosition

      batch.spriteBatch.draw(
        icons(iconPosX)(iconPosY),
        mousePosition.x - InventoryData.slotSize / 2,
        mousePosition.y - InventoryData.slotSize / 2,
        InventoryData.slotSize,
        InventoryData.slotSize
      )
    }

    if (gameState.inventoryWindow.equipmentItemBeingMoved.nonEmpty) {
      val (iconPosX, iconPosY) = equipment(gameState.inventoryWindow.equipmentItemBeingMoved.get).template.iconPosition

      batch.spriteBatch.draw(
        icons(iconPosX)(iconPosY),
        mousePosition.x - InventoryData.slotSize / 2,
        mousePosition.y - InventoryData.slotSize / 2,
        InventoryData.slotSize,
        InventoryData.slotSize
      )
    }
  }

  def renderDescription(gameState: GameState, batch: RendererBatch, mousePosition: Vector2Wrapper): Unit = {
    val player = gameState.player

    val x: Float = mousePosition.x
    val y: Float = mousePosition.y

    var inventorySlotMousedOver: Option[Int] = None
    var equipmentSlotMousedOver: Option[Int] = None

    InventoryData.inventoryRectangles
      .filter { case (_, v) => v.contains(x, y) }
      .foreach { case (k, _) => inventorySlotMousedOver = Some(k) }

    InventoryData.equipmentRectangles
      .filter { case (_, v) => v.contains(x, y) }
      .foreach { case (k, _) => equipmentSlotMousedOver = Some(k) }

    val item = (inventorySlotMousedOver, equipmentSlotMousedOver) match {
      case (Some(index), _)
          if gameState.inventoryWindow.inventoryItemBeingMoved.isEmpty || index != gameState.inventoryWindow.inventoryItemBeingMoved.get =>
        player.params.inventoryItems.get(index)
      case (_, Some(index))
          if gameState.inventoryWindow.equipmentItemBeingMoved.isEmpty || index != gameState.inventoryWindow.equipmentItemBeingMoved.get =>
        player.params.equipmentItems.get(index)
      case _ => None
    }

    if (item.nonEmpty) {

      Assets.defaultFont.draw(
        batch.spriteBatch,
        item.get.template.name,
        InventoryData.backgroundRect.x + InventoryData.margin,
        InventoryData.backgroundRect.y + InventoryData.backgroundRect.height - (InventoryData.inventoryHeight + 5),
        Color.DARK_GRAY
      )

      Assets.defaultFont.draw(
        batch.spriteBatch,
        item.get.itemInformation(),
        InventoryData.backgroundRect.x + InventoryData.margin,
        InventoryData.backgroundRect.y + InventoryData.backgroundRect.height - (InventoryData.inventoryHeight + 35),
        Color.DARK_GRAY
      )
    }

  }
}
