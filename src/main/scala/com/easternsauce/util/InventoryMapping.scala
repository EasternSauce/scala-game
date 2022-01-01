package com.easternsauce.util

object InventoryMapping {
  val equipmentTypes = Map(
    0 -> "weapon",
    1 -> "weapon",
    2 -> "helmet",
    3 -> "body",
    4 -> "gloves",
    5 -> "ring",
    6 -> "boots",
    7 -> "consumable"
  )
  val equipmentTypeNames = Map(
    0 -> "Primary Weapon",
    1 -> "Secondary Weapon",
    2 -> "Helmet",
    3 -> "Body",
    4 -> "Gloves",
    5 -> "Ring",
    6 -> "Boots",
    7 -> "Consumable"
  )

  val primaryWeaponIndex: Int = (for ((k, v) <- equipmentTypeNames) yield (v, k))("Primary Weapon")
  val secondaryWeaponIndex: Int = (for ((k, v) <- equipmentTypeNames) yield (v, k))("Secondary Weapon")
  val consumableIndex: Int = (for ((k, v) <- equipmentTypeNames) yield (v, k))("Consumable")

}
