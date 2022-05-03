package com.easternsauce.view.physics.terrain

import com.badlogic.gdx.physics.box2d.{Body, World}
import com.easternsauce.model.GameState
import com.easternsauce.view.physics.B2BodyFactory

case class LootPileBody(areaId: String, lootPileId: String) {

  val width = 1.5f
  val height = 1.5f

  var b2Body: Body = _

  def init(terrains: Map[String, Terrain], gameState: GameState): Unit = {
    val lootPile = gameState.areas(areaId).params.lootPiles(lootPileId)

    b2Body = B2BodyFactory.createLootPileB2body(terrains(areaId).world, this, lootPile.x, lootPile.y)

  }

  def destroy(world: World): Unit = {
    world.destroyBody(b2Body)
  }

}
