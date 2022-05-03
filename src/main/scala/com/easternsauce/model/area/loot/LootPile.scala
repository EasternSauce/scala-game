package com.easternsauce.model.area.loot

import com.easternsauce.model.item.Item

case class LootPile(x: Float, y: Float, items: List[Item] = List(), isTreasure: Boolean = false) {}
