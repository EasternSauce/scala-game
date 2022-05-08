package com.easternsauce.model.area

import com.easternsauce.model.area.loot.LootPile

case class AreaParams(lootPiles: Map[String, LootPile] = Map(), lootPileCounter: Int = 0)
