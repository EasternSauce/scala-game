package com.easternsauce.view.renderer.terrain

import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.easternsauce.model.GameState
import com.easternsauce.system.Assets
import com.easternsauce.util.RendererBatch
import com.easternsauce.view.physics.terrain.AreaGate

case class AreaGateRenderer(areaGate: AreaGate) {
  private val downArrowImageFrom = new Image(Assets.atlas.findRegion("downarrow"))
  private val downArrowImageTo = new Image(Assets.atlas.findRegion("downarrow"))

  downArrowImageFrom.setPosition(areaGate.fromPosX - areaGate.width / 2f, areaGate.fromPosY - areaGate.height / 2f)
  downArrowImageTo.setPosition(areaGate.toPosX - areaGate.width / 2f, areaGate.toPosY - areaGate.height / 2f)
  downArrowImageFrom.setWidth(areaGate.width)
  downArrowImageFrom.setHeight(areaGate.height)
  downArrowImageTo.setWidth(areaGate.width)
  downArrowImageTo.setHeight(areaGate.height)

  def render(gameState: GameState, batch: RendererBatch): Unit = {
    val areaId = gameState.currentAreaId

    if (areaId == areaGate.areaFrom) downArrowImageFrom.draw(batch.spriteBatch, 1.0f)
    if (areaId == areaGate.areaTo) downArrowImageTo.draw(batch.spriteBatch, 1.0f)
  }

}
