package com.easternsauce.model.hud

case class InventoryWindow(
  inventoryItemBeingMoved: Option[Int] = None,
  equipmentItemBeingMoved: Option[Int] = None,
  isOpen: Boolean = false
)
