package com.easternsauce.view.renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.{Rectangle, Vector2, Vector3}
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.easternsauce.model.GameState
import com.easternsauce.system.Assets
import com.easternsauce.system.Assets._
import com.easternsauce.util.{InventoryMapping, RendererBatch}

import scala.collection.mutable

case class InventoryRenderer() {

  var visible = false
  var inventoryItemBeingMoved: Option[Int] = None
  var equipmentItemBeingMoved: Option[Int] = None

  val backgroundImage = new Image(Assets.atlas.findRegion("background2"))

  val icons: Array[Array[TextureRegion]] = Assets.atlas.findRegion("nice_icons").split(32, 32)

  private val backgroundRect: Rectangle = new Rectangle(
    Gdx.graphics.getWidth * 0.2f,
    Gdx.graphics.getHeight * 0.3f,
    Gdx.graphics.getWidth * 0.6f,
    Gdx.graphics.getHeight * 0.6f
  )

  private val backgroundOuterRect: Rectangle = new Rectangle(
    backgroundRect.x - Gdx.graphics.getWidth * 0.1f,
    backgroundRect.y - Gdx.graphics.getHeight * 0.1f,
    backgroundRect.width + Gdx.graphics.getWidth * 0.2f,
    backgroundRect.height + Gdx.graphics.getHeight * 0.2f
  )

  backgroundImage.setBounds(
    backgroundOuterRect.x,
    backgroundOuterRect.y,
    backgroundOuterRect.width,
    backgroundOuterRect.height
  )

  private val totalRows = 5
  private val totalColumns = 8
  val inventoryTotalSlots: Int = totalRows * totalColumns
  private val margin = 20
  private val slotSize = 40f
  private val spaceBetweenSlots = 12
  private val spaceBeforeEquipment = 270

  private val inventoryWidth = margin + (slotSize + spaceBetweenSlots) * totalColumns
  private val inventoryHeight = margin + (slotSize + spaceBetweenSlots) * totalRows

  private val inventoryRectangles: mutable.Map[Int, Rectangle] = mutable.Map()

  private val equipmentTotalSlots = 8
  private val equipmentRectangles: mutable.Map[Int, Rectangle] = mutable.Map()

  defineSlotRectangles()

  private def defineSlotRectangles(): Unit = {
    for (i <- 0 until inventoryTotalSlots) {
      inventoryRectangles += (i -> new Rectangle(
        inventorySlotPositionX(i),
        inventorySlotPositionY(i),
        slotSize,
        slotSize
      ))
    }

    for (i <- 0 until equipmentTotalSlots) {
      equipmentRectangles += (i -> new Rectangle(
        equipmentSlotPositionX(i),
        equipmentSlotPositionY(i),
        slotSize,
        slotSize
      ))
    }

  }

  def render(gameState: GameState, batch: RendererBatch, mousePosition: Vector2): Unit = {
    if (visible) {
      backgroundImage.draw(batch.spriteBatch, 1.0f)

      inventoryRectangles.values.foreach(rect => {
        batch.shapeDrawer.filledRectangle(rect.x - 3, rect.y - 3, rect.width + 6, rect.height + 6, Color.BROWN)
        batch.shapeDrawer.filledRectangle(rect, Color.BLACK)
      })

      equipmentRectangles.foreach {
        case (index, rect) =>
          batch.shapeDrawer.filledRectangle(rect.x - 3, rect.y - 3, rect.width + 6, rect.height + 6, Color.BROWN)
          batch.shapeDrawer.filledRectangle(rect, Color.BLACK)
          Assets.defaultFont.draw(
            batch.spriteBatch,
            InventoryMapping.equipmentTypeNames(index) + ":",
            rect.x - slotSize / 2 - 170,
            rect.y + slotSize / 2 + 7,
            Color.DARK_GRAY
          )
      }

      renderPlayerItems(gameState, batch, mousePosition)
      renderDescription(gameState, batch, mousePosition)
    }

  }

  def renderPlayerItems(gameState: GameState, batch: RendererBatch, mousePosition: Vector2): Unit = {
    val player = gameState.player

    val items = player.params.inventoryItems
    val equipment = player.params.equipmentItems

    items
      .filterNot {
        case (index, _) => if (inventoryItemBeingMoved.nonEmpty) inventoryItemBeingMoved.get == index else false
      }
      .foreach {
        case (index, item) =>
          val (iconPosX, iconPosY) = item.template.iconPosition
          val textureRegion = icons(iconPosY)(iconPosX)
          val x = inventorySlotPositionX(index)
          val y = inventorySlotPositionY(index)
          batch.spriteBatch.draw(textureRegion, x, y, slotSize, slotSize)

          if (item.quantity > 1) {
            Assets.defaultFont.draw(batch.spriteBatch, item.quantity.toString, x, y + 15, Color.WHITE)
          }
      }

    equipment
      .filterNot {
        case (index, _) => if (equipmentItemBeingMoved.nonEmpty) equipmentItemBeingMoved.get == index else false
      }
      .foreach {
        case (index, item) =>
          val (iconPosX, iconPosY) = item.template.iconPosition
          val textureRegion = icons(iconPosY)(iconPosX)
          val x = equipmentSlotPositionX(index)
          val y = equipmentSlotPositionY(index)
          batch.spriteBatch.draw(textureRegion, x, y, slotSize, slotSize)

          if (item.quantity > 1) {
            Assets.defaultFont.draw(batch.spriteBatch, item.quantity.toString, x, y + 15, Color.WHITE)
          }
      }

    val x: Float = mousePosition.x
    val y: Float = mousePosition.y

    val (iconPosX, iconPosY) = items(inventoryItemBeingMoved.get).template.iconPosition

    if (inventoryItemBeingMoved.nonEmpty) {
      batch.spriteBatch.draw(icons(iconPosY)(iconPosX), x - slotSize / 2, y - slotSize / 2, slotSize, slotSize)
    }

    if (equipmentItemBeingMoved.nonEmpty) {
      batch.spriteBatch.draw(icons(iconPosY)(iconPosX), x - slotSize / 2, y - slotSize / 2, slotSize, slotSize)
    }
  }

  def renderDescription(gameState: GameState, batch: RendererBatch, mousePosition: Vector2): Unit = {
    val player = gameState.player

    val x: Float = mousePosition.x
    val y: Float = mousePosition.y

    var inventorySlotMousedOver: Option[Int] = None
    var equipmentSlotMousedOver: Option[Int] = None

    inventoryRectangles
      .filter { case (_, v) => v.contains(x, y) }
      .foreach { case (k, _) => inventorySlotMousedOver = Some(k) }

    equipmentRectangles
      .filter { case (_, v) => v.contains(x, y) }
      .foreach { case (k, _) => equipmentSlotMousedOver = Some(k) }

    val item = (inventorySlotMousedOver, equipmentSlotMousedOver) match {
      case (Some(index), _) if inventoryItemBeingMoved.isEmpty || index != inventoryItemBeingMoved.get =>
        player.params.inventoryItems.get(index)
      case (_, Some(index)) if equipmentItemBeingMoved.isEmpty || index != equipmentItemBeingMoved.get =>
        player.params.equipmentItems.get(index)
      case _ => None
    }

    if (item.nonEmpty) {

      Assets.defaultFont.draw(
        batch.spriteBatch,
        item.get.template.name,
        backgroundRect.x + margin,
        backgroundRect.y + backgroundRect.height - (inventoryHeight + 5),
        Color.DARK_GRAY
      )

      Assets.defaultFont.draw(
        batch.spriteBatch,
        item.get.itemInformation(),
        backgroundRect.x + margin,
        backgroundRect.y + backgroundRect.height - (inventoryHeight + 35),
        Color.DARK_GRAY
      )
    }

  }

  private def inventorySlotPositionX(index: Int): Float = {
    val currentColumn = index % totalColumns
    backgroundRect.x + margin + (slotSize + spaceBetweenSlots) * currentColumn
  }

  private def inventorySlotPositionY(index: Int): Float = {
    val currentRow = index / totalColumns
    backgroundRect.y + backgroundRect.height - (slotSize + margin + (slotSize + spaceBetweenSlots) * currentRow)
  }

  private def equipmentSlotPositionX(index: Int): Float = {
    backgroundRect.x + inventoryWidth + margin + spaceBeforeEquipment
  }

  private def equipmentSlotPositionY(index: Int): Float = {
    backgroundRect.y + backgroundRect.height - (slotSize + margin + (slotSize + spaceBetweenSlots) * index)
  }

  def moveItemClick(gameState: GameState, mousePosition: Vector2): Unit = {
    val player = gameState.player

    var inventorySlotClicked: Option[Int] = None
    var equipmentSlotClicked: Option[Int] = None

    val x: Float = mousePosition.x
    val y: Float = mousePosition.y

    if (backgroundOuterRect.contains(x, y)) {
      inventoryRectangles
        .filter { case (_, v) => v.contains(x, y) }
        .foreach { case (k, _) => inventorySlotClicked = Some(k) }

      equipmentRectangles
        .filter { case (_, v) => v.contains(x, y) }
        .foreach { case (k, _) => equipmentSlotClicked = Some(k) }

      (inventoryItemBeingMoved, equipmentItemBeingMoved, inventorySlotClicked, equipmentSlotClicked) match {
        case (Some(from), _, Some(to), _) => swapInventorySlotContent(gameState, from, to)
        case (Some(from), _, _, Some(to)) => swapBetweenInventoryAndEquipment(gameState, from, to)
        case (_, Some(from), Some(to), _) => swapBetweenInventoryAndEquipment(gameState, to, from)
        case (_, Some(from), _, Some(to)) => swapEquipmentSlotContent(gameState, from, to)
        case (_, _, Some(index), _) =>
          if (player.params.inventoryItems.contains(index)) inventoryItemBeingMoved = Some(index)
        case (_, _, _, Some(index)) =>
          if (player.params.equipmentItems.contains(index)) equipmentItemBeingMoved = Some(index)
        case _ =>
          inventoryItemBeingMoved = None
          equipmentItemBeingMoved = None
      }
    } else {
      if (inventoryItemBeingMoved.nonEmpty) {
        val item = player.params.inventoryItems(inventoryItemBeingMoved.get)
        //areaMap(currentAreaId.get).spawnLootPile(player.pos.x, player.pos.y, item)  TODO: spawn lootpile

        //player.params.inventoryItems.remove(inventoryItemBeingMoved.get) TODO: remove item

        Assets.sound("coinBag").play(0.3f)

        inventoryItemBeingMoved = None
      }
      if (equipmentItemBeingMoved.nonEmpty) {
        val item = player.params.inventoryItems(equipmentItemBeingMoved.get)
        //areaMap(currentAreaId.get).spawnLootPile(player.pos.x, player.pos.y, item) TODO: spawn lootpile

        //player.params.equipmentItems.remove(equipmentItemBeingMoved.get) TODO: remove item

        Assets.sound("coinBag").play(0.3f)

        equipmentItemBeingMoved = None
      }
      //player.promoteSecondaryToPrimaryWeapon() TODO: promote weapon
    }

  }

  def swapInventorySlotContent(gameState: GameState, fromIndex: Int, toIndex: Int): Unit = {
    val player = gameState.player

    val itemFrom = player.params.inventoryItems.get(fromIndex)
    val itemTo = player.params.inventoryItems.get(toIndex)

    val temp = itemTo

//    if (itemFrom.nonEmpty) player.params.inventoryItems(toIndex) = itemFrom.get TODO
//    else player.params.inventoryItems.remove(toIndex)
//    if (temp.nonEmpty) player.params.inventoryItems(fromIndex) = temp.get
//    else player.params.inventoryItems.remove(fromIndex)

    inventoryItemBeingMoved = None
    equipmentItemBeingMoved = None
  }

  def swapEquipmentSlotContent(gameState: GameState, fromIndex: Int, toIndex: Int): Unit = {
    val player = gameState.player

    val itemFrom = player.params.equipmentItems.get(fromIndex)
    val itemTo = player.params.equipmentItems.get(toIndex)

    val temp = itemTo

    val fromEquipmentTypeMatches =
      itemFrom.nonEmpty && itemFrom.get.template.parameters("equipableType").stringValue.get == InventoryMapping
        .equipmentTypes(toIndex)
    val toEquipmentTypeMatches =
      itemTo.nonEmpty && itemTo.get.template.parameters("equipableType").stringValue.get == InventoryMapping
        .equipmentTypes(fromIndex)

//    if (fromEquipmentTypeMatches && toEquipmentTypeMatches) { TODO
//      if (itemFrom.nonEmpty) player.params.equipmentItems(toIndex) = itemFrom.get
//      else player.params.equipmentItems.remove(toIndex)
//      if (temp.nonEmpty) player.params.equipmentItems(fromIndex) = temp.get
//      else player.params.equipmentItems.remove(fromIndex)
//    }

    inventoryItemBeingMoved = None
    equipmentItemBeingMoved = None
  }

  def swapBetweenInventoryAndEquipment(gameState: GameState, inventoryIndex: Int, equipmentIndex: Int): Unit = {
    val player = gameState.player

    val inventoryItem = player.params.inventoryItems.get(inventoryIndex)
    val equipmentItem = player.params.equipmentItems.get(equipmentIndex)

    val temp = equipmentItem

    val equipmentTypeMatches =
      inventoryItem.nonEmpty && inventoryItem.get.template
        .parameters("equipableType")
        .stringValue
        .get == InventoryMapping.equipmentTypes(equipmentIndex)
//
//    if (inventoryItem.isEmpty || equipmentTypeMatches) { TODO
//      if (temp.nonEmpty) player.inventoryItems(inventoryIndex) = temp.get
//      else player.params.inventoryItems.remove(inventoryIndex)
//      if (inventoryItem.nonEmpty) player.equipmentItems(equipmentIndex) = inventoryItem.get
//      else player.params.equipmentItems.remove(equipmentIndex)
//    }
//
//    player.promoteSecondaryToPrimaryWeapon()

    inventoryItemBeingMoved = None
    equipmentItemBeingMoved = None
  }

  def dropSelectedItem(gameState: GameState, mousePosition: Vector3): Unit = {
    val player = gameState.player

    val x: Float = mousePosition.x
    val y: Float = mousePosition.y

    var inventorySlotHovered: Option[Int] = None
    var equipmentSlotHovered: Option[Int] = None

    inventoryRectangles
      .filter { case (_, v) => v.contains(x, y) }
      .foreach { case (k, _) => inventorySlotHovered = Some(k) }

    equipmentRectangles
      .filter { case (_, v) => v.contains(x, y) }
      .foreach { case (k, _) => equipmentSlotHovered = Some(k) }

//    if (inventorySlotHovered.nonEmpty && player.params.inventoryItems.contains(inventorySlotHovered.get)) { TODO
//      areaMap(currentAreaId.get)
//        .spawnLootPile(player.pos.x, player.pos.y, player.params.inventoryItems(inventorySlotHovered.get))
//      player.params.inventoryItems.remove(inventorySlotHovered.get)
//
//      Assets.sound("coinBag").play(0.3f)
//
//      player.promoteSecondaryToPrimaryWeapon()
//
//    }

    if (equipmentSlotHovered.nonEmpty && player.params.equipmentItems.contains(inventorySlotHovered.get)) {
//      areaMap(currentAreaId.get) TODO
//        .spawnLootPile(player.pos.x, player.pos.y, player.params.equipmentItems(equipmentSlotHovered.get))
//      player.equipmentItems.remove(equipmentSlotHovered.get)

      Assets.sound("coinBag").play(0.3f)
    }
  }

  def useItemClick(gameState: GameState, mousePosition: Vector3): Unit = {
    val player = gameState.player

    val x: Float = mousePosition.x
    val y: Float = mousePosition.y

    var inventorySlotHovered: Option[Int] = None
    var equipmentSlotHovered: Option[Int] = None

    inventoryRectangles
      .filter { case (_, v) => v.contains(x, y) }
      .foreach { case (k, _) => inventorySlotHovered = Some(k) }

    equipmentRectangles
      .filter { case (_, v) => v.contains(x, y) }
      .foreach { case (k, _) => equipmentSlotHovered = Some(k) }

    if (inventorySlotHovered.nonEmpty && player.params.inventoryItems.contains(inventorySlotHovered.get)) {
      val item = player.params.inventoryItems.get(inventorySlotHovered.get)
//      if (item.nonEmpty && item.get.template.consumable.get) { TODO
//        player.useItem(item.get)
//        if (item.get.quantity <= 1) player.params.inventoryItems.remove(inventorySlotHovered.get)
//        else item.get.quantity = item.get.quantity - 1
//      }
    }

    if (equipmentSlotHovered.nonEmpty && player.params.equipmentItems.contains(equipmentSlotHovered.get)) {
      val item = player.params.equipmentItems.get(equipmentSlotHovered.get)
//      if (item.nonEmpty && item.get.template.consumable.get) { TODO
//        player.useItem(item.get)
//        if (item.get.quantity <= 1) player.params.equipmentItems.remove(equipmentSlotHovered.get)
//        else item.get.quantity = item.get.quantity - 1
//      }
    }

  }
}
