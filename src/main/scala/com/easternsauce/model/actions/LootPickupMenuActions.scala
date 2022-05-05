package com.easternsauce.model.actions

import com.easternsauce.hud.LootPileMenuConfig
import com.easternsauce.model.GameState
import com.easternsauce.model.event.{UpdatePhysicsOnLootPileDespawnEvent, UpdateRendererOnLootPileDespawnEvent}
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
      case ((_, _, _), i) => LootPileMenuConfig.menuOptionRect(i).contains(mousePos.x, mousePos.y)
    }

    // TODO: treasures

    if (clickedItem.nonEmpty) {
      val ((areaId, lootPileId, item), i) = clickedItem.get

      if (this.player.tryPickUpItem(item)) {
        val lootPile = this.areas(areaId).params.lootPiles(lootPileId)

        this
          .modify(_.areas.at(areaId).params.lootPiles.at(lootPileId).items)
          .using(_.filterNot(Set(item)))
          .pipe(
            gameState =>
              if (lootPile.items.size == 1)
                gameState
                  .modify(_.areas.at(areaId).params.lootPiles)
                  .using(_.filterNot(Set(lootPile)))
                  .modify(_.events)
                  .setTo(
                    List(
                      UpdateRendererOnLootPileDespawnEvent(areaId, lootPileId),
                      UpdatePhysicsOnLootPileDespawnEvent(areaId, lootPileId)
                    ) ::: gameState.events
                  )
              else gameState
          )

      } else this
    } else this
  }

}
