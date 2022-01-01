package com.easternsauce.model.item

import com.softwaremill.quicklens._

case class ItemTemplate(
  id: String,
  name: String,
  description: String,
  iconPosition: (Int, Int),
  parameters: Map[String, ItemParameterValue] = Map(
    "stackable" -> ItemParameterValue(false),
    "consumable" -> ItemParameterValue(false),
    "equipable" -> ItemParameterValue(false),
    "equipableType" -> ItemParameterValue(),
    "worth" -> ItemParameterValue(0),
    "damage" -> ItemParameterValue(),
    "armor" -> ItemParameterValue(),
    "poisonChance" -> ItemParameterValue(0f),
    "attackType" -> ItemParameterValue(),
    "attackScale" -> ItemParameterValue(1.0f),
    "attackSpeed" -> ItemParameterValue(1.0f)
  )
) {

  def worth: Option[Int] = parameters("worth").intValue

  def armor: Option[Int] = parameters("armor").intValue

  def damage: Option[Int] = parameters("damage").intValue

  def attackScale: Option[Float] = parameters("attackScale").floatValue

  def attackType: Option[String] = parameters("attackType").stringValue

  def stackable: Option[Boolean] = parameters("stackable").boolValue

  def consumable: Option[Boolean] = parameters("consumable").boolValue

  def poisonChance: Option[Float] = parameters("poisonChance").floatValue

  def attackSpeed: Option[Float] = parameters("attackSpeed").floatValue

  def setWorth(worth: Int): ItemTemplate = {
    this.modify(_.parameters.at("worth")).setTo(ItemParameterValue(worth))
  }

  def setEquipable(equipable: Boolean): ItemTemplate = {
    this.modify(_.parameters.at("equipable")).setTo(ItemParameterValue(equipable))
  }

  def setEquipableType(equipableType: String): ItemTemplate = {
    this.modify(_.parameters.at("equipableType")).setTo(ItemParameterValue(equipableType))
  }

  def setArmor(armor: Int): ItemTemplate = {
    this.modify(_.parameters.at("armor")).setTo(ItemParameterValue(armor))
  }

  def setDamage(damage: Int): ItemTemplate = {
    this.modify(_.parameters.at("damage")).setTo(ItemParameterValue(damage))
  }

  def setAttackType(attackType: String): ItemTemplate = {
    this.modify(_.parameters.at("attackType")).setTo(ItemParameterValue(attackType))
  }

  def setAttackScale(attackScale: Float): ItemTemplate = {
    this.modify(_.parameters.at("attackScale")).setTo(ItemParameterValue(attackScale))
  }

  def setAttackSpeed(attackSpeed: Float): ItemTemplate = {
    this.modify(_.parameters.at("attackSpeed")).setTo(ItemParameterValue(attackSpeed))
  }

  def setPoisonChance(poisonChance: Float): ItemTemplate = {
    this.modify(_.parameters.at("poisonChance")).setTo(ItemParameterValue(poisonChance))
  }

  def setStackable(stackable: Boolean): ItemTemplate = {
    this.modify(_.parameters.at("stackable")).setTo(ItemParameterValue(stackable))
  }

  def setConsumable(consumable: Boolean): ItemTemplate = {
    this.modify(_.parameters.at("consumable")).setTo(ItemParameterValue(consumable))
  }

}

object ItemTemplate {

  val templates: Map[String, ItemTemplate] = {
    List(
      ItemTemplate("leatherArmor", "Leather Armor", "-", (7, 8))
        .setWorth(150)
        .setEquipable(true)
        .setEquipableType("body")
        .setArmor(13),
      ItemTemplate("ringmailGreaves", "Ringmail Greaves", "-", (8, 3))
        .setWorth(50)
        .setEquipable(true)
        .setEquipableType("boots")
        .setArmor(7),
      ItemTemplate("hideGloves", "Hide Gloves", "-", (8, 0))
        .setWorth(70)
        .setEquipable(true)
        .setEquipableType("gloves")
        .setArmor(5),
      ItemTemplate("crossbow", "Crossbow", "-", (6, 4))
        .setWorth(500)
        .setEquipable(true)
        .setEquipableType("weapon")
        .setDamage(45)
        .setAttackType("shoot_arrow"),
      ItemTemplate("ironSword", "Iron Sword", "-", (5, 2))
        .setWorth(100)
        .setEquipable(true)
        .setEquipableType("weapon")
        .setDamage(60)
        .setAttackType("slash")
        .setAttackScale(2.4f)
        .setAttackSpeed(0.7f),
      ItemTemplate("woodenSword", "Wooden Sword", "-", (5, 0))
        .setWorth(70)
        .setEquipable(true)
        .setEquipableType("weapon")
        .setDamage(45)
        .setAttackType("slash")
        .setAttackScale(1.8f)
        .setAttackSpeed(1.3f),
      ItemTemplate("leatherHelmet", "Leather Helmet", "-", (7, 2))
        .setWorth(80)
        .setEquipable(true)
        .setEquipableType("helmet")
        .setArmor(9),
      ItemTemplate("lifeRing", "Life Ring", "Increases life when worn", (8, 5))
        .setWorth(1000)
        .setEquipable(true)
        .setEquipableType("ring"),
      ItemTemplate("poisonDagger", "Poison Dagger", "-", (5, 6))
        .setWorth(500)
        .setEquipable(true)
        .setEquipableType("weapon")
        .setDamage(25)
        .setAttackType("slash")
        .setAttackSpeed(3.0f)
        .setAttackScale(1.2f)
        .setPoisonChance(0.35f),
      ItemTemplate("healingPowder", "Healing Powder", "Quickly regenerates life", (20, 5))
        .setWorth(45)
        .setEquipable(true)
        .setEquipableType("consumable")
        .setStackable(true)
        .setConsumable(true),
      ItemTemplate("trident", "Trident", "-", (5, 8))
        .setWorth(900)
        .setEquipable(true)
        .setEquipableType("weapon")
        .setDamage(85)
        .setAttackType("thrust")
        .setAttackScale(2.0f)
        .setAttackSpeed(1.3f),
      ItemTemplate("steelArmor", "Steel Armor", "-", (7, 4))
        .setWorth(200)
        .setEquipable(true)
        .setEquipableType("body")
        .setArmor(20),
      ItemTemplate("steelGreaves", "Steel Greaves", "-", (8, 3))
        .setWorth(150)
        .setEquipable(true)
        .setEquipableType("boots")
        .setArmor(13),
      ItemTemplate("steelGloves", "Steel Gloves", "-", (8, 1))
        .setWorth(130)
        .setEquipable(true)
        .setEquipableType("gloves")
        .setArmor(10),
      ItemTemplate("steelHelmet", "Steel Helmet", "-", (7, 1))
        .setWorth(170)
        .setEquipable(true)
        .setEquipableType("helmet")
        .setArmor(15),
      ItemTemplate("demonTrident", "Trident", "-", (5, 8))
        .setWorth(900)
        .setEquipable(true)
        .setEquipableType("weapon")
        .setDamage(85)
        .setAttackType("thrust")
        .setAttackScale(3.0f)
        .setAttackSpeed(1.5f),
      ItemTemplate("lifeRing", "Life Ring", "Increases life when worn", (8, 5))
        .setWorth(1400)
        .setEquipable(true)
        .setEquipableType("ring")
    ).map(itemTemplate => itemTemplate.id -> itemTemplate).toMap
  }
}
