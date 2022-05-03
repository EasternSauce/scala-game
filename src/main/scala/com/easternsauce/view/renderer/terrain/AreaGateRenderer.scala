package com.easternsauce.view.renderer.terrain

import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.easternsauce.model.GameState
import com.easternsauce.system.Assets
import com.easternsauce.util.RendererBatch
import com.easternsauce.view.physics.terrain.AreaGateBody

case class AreaGateRenderer(areaGate: AreaGateBody) {
  private val downArrowImageFrom = new Image(Assets.atlas.findRegion("downarrow"))
  private val downArrowImageTo = new Image(Assets.atlas.findRegion("downarrow"))

  downArrowImageFrom.setPosition(areaGate.x1 - areaGate.width / 2f, areaGate.y1 - areaGate.height / 2f)
  downArrowImageTo.setPosition(areaGate.x2 - areaGate.width / 2f, areaGate.y2 - areaGate.height / 2f)
  downArrowImageFrom.setWidth(areaGate.width)
  downArrowImageFrom.setHeight(areaGate.height)
  downArrowImageTo.setWidth(areaGate.width)
  downArrowImageTo.setHeight(areaGate.height)

  def render(gameState: GameState, batch: RendererBatch): Unit = {
    val areaId = gameState.currentAreaId

    if (areaId == areaGate.area1Id) downArrowImageFrom.draw(batch.spriteBatch, 1.0f)
    if (areaId == areaGate.area2Id) downArrowImageTo.draw(batch.spriteBatch, 1.0f)
  }

}
