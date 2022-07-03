package com.easternsauce.model.actions

import com.easternsauce.helper.LootPickupMenuHelper
import com.easternsauce.model.GameState
import com.easternsauce.model.event.{PlaySoundEvent, UpdatePhysicsOnLootPileDespawnEvent, UpdateRendererOnLootPileDespawnEvent}
import com.easternsauce.model.item.Item
import com.easternsauce.model.util.EnhancedChainingSyntax.enhancedScalaUtilChainingOps
import com.easternsauce.util.Vec2
import com.softwaremill.quicklens._

trait LootPickupMenuActions {
  this: GameState =>

  def lootPickupMenuClick(mousePos: Vec2): GameState = {

    val itemOptions: List[((String, String, Item), Int)] = this.lootPilePickupMenu.visibleLootPiles
      .map { case (areaId, lootPileId) => (areaId, lootPileId, this.areas(areaId).params.lootPiles(lootPileId).items) }
      .foldLeft(List[(String, String, Item)]()) {
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
          .pipeIf(lootPile.items.size == 1)(
            _.modify(_.areas.at(areaId).params.lootPiles)
              .using(_.removed(lootPileId))
              .modify(_.lootPilePickupMenu.visibleLootPiles)
              .using(_.filterNot(Set((areaId, lootPileId))))
              .modify(_.events)
              .using(
                _.prependedAll(
                  List(
                    UpdateRendererOnLootPileDespawnEvent(areaId, lootPileId),
                    UpdatePhysicsOnLootPileDespawnEvent(areaId, lootPileId)
                  )
                )
              )
          )
          .modify(_.creatures.at(this.currentPlayerId))
          .using(_.pickUpItem(item))
          .pipe(_.modify(_.events).using(_.prepended(PlaySoundEvent("coinBag"))))

      } else this
    } else this
  }

}
