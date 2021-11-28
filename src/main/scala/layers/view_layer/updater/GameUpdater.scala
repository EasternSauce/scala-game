package layers.view_layer.updater

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.{SpriteBatch, TextureAtlas}
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.{Body, BodyDef, Box2DDebugRenderer, FixtureDef, PolygonShape, World}
import layers.model_layer.gamestate.GameState
import layers.view_layer.updater.creature.CreatureRenderer

case class GameUpdater(atlas: TextureAtlas) {


  var creatureRenderers: Map[String, CreatureRenderer] = Map()


  def update(gameState: GameState, world: World): Unit = {
    val creatures = gameState.creatures

    creatures.keys.foreach{
      creatureId => if (creatureRenderers.contains(creatureId)) {
        creatureRenderers(creatureId).update(gameState, world)
      }
      else {
        val newRenderer = CreatureRenderer(creatureId, atlas)
        creatureRenderers = creatureRenderers + (creatureId -> newRenderer)
        newRenderer.init(gameState, world)
        newRenderer.update(gameState, world)
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
