package com.easternsauce.model.hud

case class LootPilePickupMenu(visibleLootPiles: List[(String, String)] = List()) {

  def isOpen: Boolean = visibleLootPiles.nonEmpty
}
