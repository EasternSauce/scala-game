package com.easternsauce.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.{TiledMap, TmxMapLoader}
import com.easternsauce.box2d_physics.PhysicsController
import com.easternsauce.model.GameState
import com.easternsauce.model.area.Area
import com.easternsauce.model.creature.{CreatureParams, Player, Skeleton}
import com.easternsauce.screen.PlayScreen
import com.easternsauce.util.RendererBatch
import com.easternsauce.view.GameView

class MyGdxGame extends Game {

  private var worldBatch: RendererBatch = _
  private var hudBatch: RendererBatch = _

  var atlas: TextureAtlas = _

  var gameState: GameState = _
  var gameView: GameView = _

  private val mapScale = 4.0f

  val mapsToLoad = Map("area1" -> "assets/areas/area1", "area2" -> "assets/areas/area2")

  val mapLoader: TmxMapLoader = new TmxMapLoader()

  var maps: Map[String, TiledMap] = _

  var physicsController: PhysicsController = _

  override def create(): Unit = {
    worldBatch = RendererBatch()
    hudBatch = RendererBatch()

    atlas = new TextureAtlas("assets/atlas/packed_atlas.atlas")

    val player: Player = Player(
      CreatureParams(
        id = "player",
        posX = 10,
        posY = 10,
        areaId = "area1",
        life = 62f,
        maxLife = 100f,
        stamina = 100f,
        maxStamina = 100f
      )
    )

    val skeleton: Skeleton = Skeleton(
      CreatureParams(
        id = "skel",
        posX = 15,
        posY = 10,
        areaId = "area1",
        life = 100f,
        maxLife = 100f,
        stamina = 100f,
        maxStamina = 100f
      )
    )

    maps = mapsToLoad.map {
      case (areaId, directory) => areaId -> mapLoader.load(directory + "/tile_map.tmx")
    }

    val areas = maps.map { case (key, _) => (key, Area()) }

    gameState = GameState(
      player = player,
      nonPlayers = Map(skeleton.params.id -> skeleton),
      currentAreaId = "area1",
      areas = areas
    )
    gameView = GameView(atlas)
    physicsController = PhysicsController()

    gameView.init(gameState, maps, mapScale)
    physicsController.init(gameState, maps, mapScale) // areaid.get

    val playScreen = new PlayScreen(worldBatch, hudBatch, gameState, gameView, physicsController)

    setScreen(playScreen)
  }

  override def dispose(): Unit = {
    worldBatch.dispose()
    hudBatch.dispose()
    gameView.dispose()
    physicsController.dispose()
  }
}
