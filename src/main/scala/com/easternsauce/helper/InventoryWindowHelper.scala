package com.easternsauce.helper

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Rectangle

object InventoryWindowHelper {
  private val totalRows = 5
  private val totalColumns = 8
  val inventoryTotalSlots: Int = totalRows * totalColumns
  val margin = 20
  val slotSize = 40f
  val spaceBetweenSlots = 12
  val spaceBeforeEquipment = 270

  val inventoryWidth: Float = margin + (slotSize + spaceBetweenSlots) * totalColumns
  val inventoryHeight: Float = margin + (slotSize + spaceBetweenSlots) * totalRows

  lazy val inventoryRectangles: Map[Int, Rectangle] = (for (i <- (0 until inventoryTotalSlots).toList)
    yield i -> new Rectangle(inventorySlotPositionX(i), inventorySlotPositionY(i), slotSize, slotSize)).toMap

  private val equipmentTotalSlots = 8

  lazy val equipmentRectangles: Map[Int, Rectangle] = (for (i <- 0 until equipmentTotalSlots)
    yield i -> new Rectangle(equipmentSlotPositionX(i), equipmentSlotPositionY(i), slotSize, slotSize)).toMap

  val backgroundRect: Rectangle = new Rectangle(
    Gdx.graphics.getWidth * 0.2f,
    Gdx.graphics.getHeight * 0.3f,
    Gdx.graphics.getWidth * 0.6f,
    Gdx.graphics.getHeight * 0.6f
  )

  def inventorySlotPositionX(index: Int): Float = {
    val currentColumn = index % totalColumns
    backgroundRect.x + margin + (slotSize + spaceBetweenSlots) * currentColumn
  }

  def inventorySlotPositionY(index: Int): Float = {
    val currentRow = index / totalColumns
    backgroundRect.y + backgroundRect.height - (slotSize + margin + (slotSize + spaceBetweenSlots) * currentRow)
  }

  def equipmentSlotPositionX(index: Int): Float = {
    backgroundRect.x + inventoryWidth + margin + spaceBeforeEquipment
  }

  def equipmentSlotPositionY(index: Int): Float = {
    backgroundRect.y + backgroundRect.height - (slotSize + margin + (slotSize + spaceBetweenSlots) * index)
  }

  val backgroundOuterRect: Rectangle = new Rectangle(
    backgroundRect.x - Gdx.graphics.getWidth * 0.1f,
    backgroundRect.y - Gdx.graphics.getHeight * 0.1f,
    backgroundRect.width + Gdx.graphics.getWidth * 0.2f,
    backgroundRect.height + Gdx.graphics.getHeight * 0.2f
  )

}
