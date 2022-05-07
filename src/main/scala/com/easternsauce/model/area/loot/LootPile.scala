package com.easternsauce.model.area.loot

import com.easternsauce.model.item.{Item, ItemTemplate}

case class LootPile(
  x: Float,
  y: Float,
  items: List[Item] =
    List(
      Item(ItemTemplate.templates("healingPowder")),
      Item(ItemTemplate.templates("woodenSword"))
    ), // TODO: placed for testing purposes
  isTreasure: Boolean = false
)
