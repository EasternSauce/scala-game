package com.easternsauce.model

case class InventoryState(
  inventoryItemBeingMoved: Option[Int] = None,
  equipmentItemBeingMoved: Option[Int] = None,
  inventoryOpen: Boolean = false
)
