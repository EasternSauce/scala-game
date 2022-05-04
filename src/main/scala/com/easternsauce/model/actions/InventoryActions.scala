package com.easternsauce.model.actions

import com.badlogic.gdx.math.Vector2
import com.easternsauce.inventory.InventoryData
import com.easternsauce.model.GameState
import com.easternsauce.system.Assets
import com.easternsauce.util.InventoryMapping
import com.softwaremill.quicklens._

import scala.util.chaining.scalaUtilChainingOps

trait InventoryActions {
  this: GameState =>

  def moveItemClick(mousePos: Vector2): GameState = {
    var inventorySlotClicked: Option[Int] = None
    var equipmentSlotClicked: Option[Int] = None

    val x: Float = mousePos.x
    val y: Float = mousePos.y

    if (InventoryData.backgroundOuterRect.contains(x, y)) {
      InventoryData.inventoryRectangles
        .filter { case (_, v) => v.contains(x, y) }
        .foreach { case (k, _) => inventorySlotClicked = Some(k) }

      InventoryData.equipmentRectangles
        .filter { case (_, v) => v.contains(x, y) }
        .foreach { case (k, _) => equipmentSlotClicked = Some(k) }

      (
        inventoryState.inventoryItemBeingMoved,
        inventoryState.equipmentItemBeingMoved,
        inventorySlotClicked,
        equipmentSlotClicked
      ) match {
        case (Some(from), _, Some(to), _) => swapInventorySlotContent(from, to)
        case (Some(from), _, _, Some(to)) => swapBetweenInventoryAndEquipment(from, to)
        case (_, Some(from), Some(to), _) => swapBetweenInventoryAndEquipment(to, from)
        case (_, Some(from), _, Some(to)) => swapEquipmentSlotContent(from, to)
        case (_, _, Some(index), _) =>
          this
            .modify(_.inventoryState.inventoryItemBeingMoved)
            .setToIf(player.params.inventoryItems.contains(index))(Some(index))
        case (_, _, _, Some(index)) =>
          this
            .modify(_.inventoryState.equipmentItemBeingMoved)
            .setToIf(player.params.equipmentItems.contains(index))(Some(index))
        case _ =>
          this
            .modifyAll(_.inventoryState.inventoryItemBeingMoved, _.inventoryState.equipmentItemBeingMoved)
            .setTo(None)
      }
    } else {
      this
        .pipe(
          gameState =>
            if (gameState.inventoryState.inventoryItemBeingMoved.nonEmpty) {
              //val item = player.params.inventoryItems(gameState.inventoryState.inventoryItemBeingMoved.get)
              //areaMap(currentAreaId.get).spawnLootPile(player.pos.x, player.pos.y, item)  TODO: spawn lootpile

              Assets.sound("coinBag").play(0.3f)

              gameState
                .modify(_.creatures.at(currentPlayerId).params.inventoryItems)
                .using(_.removed(gameState.inventoryState.inventoryItemBeingMoved.get))
                .modify(_.inventoryState.inventoryItemBeingMoved)
                .setTo(None)
            } else gameState
        )
        .pipe(
          gameState =>
            if (gameState.inventoryState.equipmentItemBeingMoved.nonEmpty) {
              //val item = player.params.inventoryItems(gameState.inventoryState.equipmentItemBeingMoved.get)
              //areaMap(currentAreaId.get).spawnLootPile(player.pos.x, player.pos.y, item) TODO: spawn lootpile

              Assets.sound("coinBag").play(0.3f)

              gameState
                .modify(_.creatures.at(currentPlayerId).params.equipmentItems)
                .using(_.removed(gameState.inventoryState.equipmentItemBeingMoved.get))
                .modify(_.inventoryState.equipmentItemBeingMoved)
                .setTo(None)
            } else gameState
        )
      //player.promoteSecondaryToPrimaryWeapon() TODO: promote weapon
    }
  }

  def swapInventorySlotContent(fromIndex: Int, toIndex: Int): GameState = {
    val itemFrom = player.params.inventoryItems.get(fromIndex)
    val itemTo = player.params.inventoryItems.get(toIndex)

    val temp = itemTo

    this
      .pipe(
        gameState =>
          if (itemFrom.nonEmpty)
            gameState
              .modify(_.creatures.at(currentPlayerId).params.inventoryItems)
              .using(_ + (toIndex -> itemFrom.get)) //toIndex
          else gameState.modify(_.creatures.at(currentPlayerId).params.inventoryItems).using(_.removed(toIndex))
      )
      .pipe(
        gameState =>
          if (temp.nonEmpty)
            gameState.modify(_.creatures.at(currentPlayerId).params.inventoryItems).using(_ + (fromIndex -> temp.get))
          else gameState.modify(_.creatures.at(currentPlayerId).params.inventoryItems).using(_.removed(fromIndex))
      )
      .modifyAll(_.inventoryState.inventoryItemBeingMoved, _.inventoryState.equipmentItemBeingMoved)
      .setTo(None)
  }

  def swapBetweenInventoryAndEquipment(inventoryIndex: Int, equipmentIndex: Int): GameState = {

    val inventoryItem = player.params.inventoryItems.get(inventoryIndex)
    val equipmentItem = player.params.equipmentItems.get(equipmentIndex)

    val temp = equipmentItem

    val equipmentTypeMatches =
      inventoryItem.nonEmpty && inventoryItem.get.template
        .parameters("equipableType")
        .stringValue
        .get == InventoryMapping.equipmentTypes(equipmentIndex)

    this
      .pipe(
        gameState =>
          if (inventoryItem.isEmpty || equipmentTypeMatches) {
            gameState
              .pipe(
                gameState =>
                  if (temp.nonEmpty)
                    gameState
                      .modify(_.creatures.at(currentPlayerId).params.inventoryItems)
                      .using(_ + (inventoryIndex -> temp.get))
                  else
                    gameState
                      .modify(_.creatures.at(currentPlayerId).params.inventoryItems)
                      .using(_.removed(inventoryIndex))
              )
              .pipe(
                gameState =>
                  if (inventoryItem.nonEmpty)
                    gameState
                      .modify(_.creatures.at(currentPlayerId).params.equipmentItems)
                      .using(_ + (equipmentIndex -> inventoryItem.get))
                  else
                    gameState
                      .modify(_.creatures.at(currentPlayerId).params.equipmentItems)
                      .using(_.removed(equipmentIndex))
              )

          } else gameState
      )
      //player.promoteSecondaryToPrimaryWeapon() TODO
      .modifyAll(_.inventoryState.inventoryItemBeingMoved, _.inventoryState.equipmentItemBeingMoved)
      .setTo(None)
  }

  def swapEquipmentSlotContent(fromIndex: Int, toIndex: Int): GameState = {

    val itemFrom = player.params.equipmentItems.get(fromIndex)
    val itemTo = player.params.equipmentItems.get(toIndex)

    val temp = itemTo

    val fromEquipmentTypeMatches =
      itemFrom.nonEmpty && itemFrom.get.template.parameters("equipableType").stringValue.get == InventoryMapping
        .equipmentTypes(toIndex)
    val toEquipmentTypeMatches =
      itemTo.nonEmpty && itemTo.get.template.parameters("equipableType").stringValue.get == InventoryMapping
        .equipmentTypes(fromIndex)

    this
      .pipe(
        gameState =>
          if (fromEquipmentTypeMatches && toEquipmentTypeMatches) {
            gameState
              .pipe(
                gameState =>
                  if (itemFrom.nonEmpty)
                    gameState
                      .modify(_.creatures.at(currentPlayerId).params.equipmentItems)
                      .using(_ + (toIndex -> itemFrom.get))
                  else gameState.modify(_.creatures.at(currentPlayerId).params.equipmentItems).using(_.removed(toIndex))
              )
              .pipe(
                gameState =>
                  if (temp.nonEmpty)
                    gameState
                      .modify(_.creatures.at(currentPlayerId).params.equipmentItems)
                      .using(_ + (fromIndex -> temp.get))
                  else
                    gameState.modify(_.creatures.at(currentPlayerId).params.equipmentItems).using(_.removed(fromIndex))
              )
          } else gameState
      )
      .modifyAll(_.inventoryState.inventoryItemBeingMoved, _.inventoryState.equipmentItemBeingMoved)
      .setTo(None)
  }

  //    def dropSelectedItem(gameState: GameState, mousePosition: Vector3): Unit = {
  //      val player = gameState.player
  //
  //      val x: Float = mousePosition.x
  //      val y: Float = mousePosition.y
  //
  //      var inventorySlotHovered: Option[Int] = None
  //      var equipmentSlotHovered: Option[Int] = None
  //
  //      InventoryData.inventoryRectangles
  //        .filter { case (_, v) => v.contains(x, y) }
  //        .foreach { case (k, _) => inventorySlotHovered = Some(k) }
  //
  //      InventoryData.equipmentRectangles
  //        .filter { case (_, v) => v.contains(x, y) }
  //        .foreach { case (k, _) => equipmentSlotHovered = Some(k) }
  //
  //      if (inventorySlotHovered.nonEmpty && player.params.inventoryItems.contains(inventorySlotHovered.get)) { TODO
  //        areaMap(currentAreaId.get)
  //          .spawnLootPile(player.pos.x, player.pos.y, player.params.inventoryItems(inventorySlotHovered.get))
  //        player.params.inventoryItems.remove(inventorySlotHovered.get)
  //
  //        Assets.sound("coinBag").play(0.3f)
  //
  //        player.promoteSecondaryToPrimaryWeapon()
  //
  //      }
  //
  //      if (equipmentSlotHovered.nonEmpty && player.params.equipmentItems.contains(inventorySlotHovered.get)) {
  //        areaMap(currentAreaId.get) TODO
  //        .spawnLootPile(player.pos.x, player.pos.y, player.params.equipmentItems(equipmentSlotHovered.get))
  //        player.equipmentItems.remove(equipmentSlotHovered.get)
  //
  //        Assets.sound("coinBag").play(0.3f)
  //      }
  //    }

  //  def useItemClick(gameState: GameState, mousePosition: Vector3): Unit = {
  //    val player = gameState.player
  //
  //    val x: Float = mousePosition.x
  //    val y: Float = mousePosition.y
  //
  //    var inventorySlotHovered: Option[Int] = None
  //    var equipmentSlotHovered: Option[Int] = None
  //
  //    InventoryData.inventoryRectangles
  //      .filter { case (_, v) => v.contains(x, y) }
  //      .foreach { case (k, _) => inventorySlotHovered = Some(k) }
  //
  //    InventoryData.equipmentRectangles
  //      .filter { case (_, v) => v.contains(x, y) }
  //      .foreach { case (k, _) => equipmentSlotHovered = Some(k) }
  //
  //    if (inventorySlotHovered.nonEmpty && player.params.inventoryItems.contains(inventorySlotHovered.get)) {
  //      val item = player.params.inventoryItems.get(inventorySlotHovered.get)
  //      if (item.nonEmpty && item.get.template.consumable.get) { TODO
  //        player.useItem(item.get)
  //        if (item.get.quantity <= 1) player.params.inventoryItems.remove(inventorySlotHovered.get)
  //        else item.get.quantity = item.get.quantity - 1
  //      }
  //    }
  //
  //    if (equipmentSlotHovered.nonEmpty && player.params.equipmentItems.contains(equipmentSlotHovered.get)) {
  //      val item = player.params.equipmentItems.get(equipmentSlotHovered.get)
  //      if (item.nonEmpty && item.get.template.consumable.get) { TODO
  //        player.useItem(item.get)
  //        if (item.get.quantity <= 1) player.params.equipmentItems.remove(equipmentSlotHovered.get)
  //        else item.get.quantity = item.get.quantity - 1
  //      }
  //    }
}
