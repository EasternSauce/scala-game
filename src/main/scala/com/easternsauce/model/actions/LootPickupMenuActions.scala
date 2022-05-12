package com.easternsauce.model.actions

import com.easternsauce.helper.LootPickupMenuHelper
import com.easternsauce.model.GameState
import com.easternsauce.model.event.{PlaySoundEvent, UpdatePhysicsOnLootPileDespawnEvent, UpdateRendererOnLootPileDespawnEvent}
import com.easternsauce.model.item.Item
import com.easternsauce.util.Vector2Wrapper
import com.softwaremill.quicklens._

import scala.util.chaining.scalaUtilChainingOps

trait LootPickupMenuActions {
  this: GameState =>

  def lootPickupMenuClick(mousePos: Vector2Wrapper): GameState = {

    val itemOptions: List[((String, String, Item), Int)] = this.lootPilePickupMenu.visibleLootPiles
      .map { case (areaId, lootPileId) => (areaId, lootPileId, this.areas(areaId).params.lootPiles(lootPileId).items) }
      .foldLeft(List[((String, String, Item))]()) {
        case (acc, (areaId, lootPileId, items)) => acc ++ items.map(item => (areaId, lootPileId, item))
      }
      .zipWithIndex

    val clickedItem = itemOptions.find {
      case ((_, _, _), i) => LootPickupMenuHelper.menuOptionRect(i).contains(mousePos.x, mousePos.y)
    }

    // TODO: treasures

    if (clickedItem.nonEmpty) {
      val ((areaId, lootPileId, item), i) = clickedItem.get

      if (this.player.canPickUpItem(item)) {
        val lootPile = this.areas(areaId).params.lootPiles(lootPileId)

        this
          .modify(_.areas.at(areaId).params.lootPiles.at(lootPileId).items)
          .using(_.filterNot(Set(item)))
          .pipe(
            gameState =>
              if (lootPile.items.size == 1)
                gameState
                  .modify(_.areas.at(areaId).params.lootPiles)
                  .using(_.removed(lootPileId))
                  .modify(_.lootPilePickupMenu.visibleLootPiles)
                  .using(_.filterNot(Set((areaId, lootPileId))))
                  .modify(_.events)
                  .setTo(
                    List(
                      UpdateRendererOnLootPileDespawnEvent(areaId, lootPileId),
                      UpdatePhysicsOnLootPileDespawnEvent(areaId, lootPileId)
                    ) ::: gameState.events
                  )
              else gameState
          )
          .modify(_.creatures.at(this.currentPlayerId))
          .using(_.pickUpItem(item))
          .pipe(gameState => gameState.modify(_.events).setTo(PlaySoundEvent("coinBag") :: gameState.events))

      } else this
    } else this
  }

}
