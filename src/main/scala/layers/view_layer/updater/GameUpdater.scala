package layers.view_layer.updater

import com.badlogic.gdx.graphics.g2d.{SpriteBatch, TextureAtlas}
import layers.model_layer.gamestate.GameState
import layers.view_layer.updater.creature.CreatureRenderer

case class GameUpdater(atlas: TextureAtlas) {

 var creatureRenderers: Map[String, CreatureRenderer] = Map()

  def update(gameState: GameState): Unit = {
    val creatures = gameState.creatures

    creatures.keys.foreach{
      creatureId => if (creatureRenderers.contains(creatureId)) {
        creatureRenderers(creatureId).update(gameState)
      }
      else {
        val newRenderer = CreatureRenderer(creatureId, atlas)
        creatureRenderers = creatureRenderers + (creatureId -> newRenderer)
        newRenderer.init(gameState)
        newRenderer.update(gameState)
      }
    }


  }

  def render(gameState: GameState, spriteBatch: SpriteBatch): Unit = {
    val creatures = gameState.creatures

    creatures.keys.foreach{
      creatureId => if (creatureRenderers.contains(creatureId)) {
        creatureRenderers(creatureId).render(spriteBatch)
      }
    }
  }
}
