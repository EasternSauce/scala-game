package com.easternsauce.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.{TiledMap, TmxMapLoader}
import com.easternsauce.view.physics.PhysicsController
import com.easternsauce.model.GameState
import com.easternsauce.model.area.Area
import com.easternsauce.model.creature.{CreatureParams, Player, Serpent, Skeleton}
import com.easternsauce.screen.PlayScreen
import com.easternsauce.system.Assets
import com.easternsauce.util.RendererBatch
import com.easternsauce.view.renderer
import com.easternsauce.view.renderer.RendererController
import io.circe.parser.decode

import java.io.FileNotFoundException

class MyGdxGame extends Game {

  private var worldBatch: RendererBatch = _
  private var hudBatch: RendererBatch = _

  var atlas: TextureAtlas = _

  var gameView: RendererController = _

  private val mapScale = 4.0f

  val mapsToLoad = Map("area1" -> "assets/areas/area1", "area2" -> "assets/areas/area2")

  val mapLoader: TmxMapLoader = new TmxMapLoader()

  var maps: Map[String, TiledMap] = _

  var physicsController: PhysicsController = _

  var playScreen: PlayScreen = _

  override def create(): Unit = {
    Assets.loadAssets()

    worldBatch = RendererBatch()
    hudBatch = RendererBatch()

    atlas = new TextureAtlas("assets/atlas/packed_atlas.atlas")

    val player: Player = Player(
      CreatureParams(
        id = "player",
        posX = 15,
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

    val wolf: Serpent = Serpent(
      CreatureParams(
        id = "zzzzzz",
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

    val gameState =
      try {
        val source = scala.io.Source.fromFile("saves/savefile.txt")
        val lines =
          try source.mkString
          finally source.close()

        import com.easternsauce.json.JsonCodecs._
        decode[GameState](lines).getOrElse(throw new RuntimeException("error decoding save file"))
      } catch {
        case _: FileNotFoundException =>
          GameState(
            player = player.init(), // TODO: init elsewhere?
            nonPlayers = Map(skeleton.params.id -> skeleton.init(), wolf.params.id -> wolf.init()),
            currentAreaId = "area1",
            areas = areas
          )

      }

    gameView = renderer.RendererController(atlas)
    physicsController = PhysicsController()

    playScreen = new PlayScreen(worldBatch, hudBatch, gameState, gameView, physicsController)

    gameView.init(gameState, maps, mapScale)
    physicsController.init(gameState, maps, mapScale)

    setScreen(playScreen)
  }

  override def dispose(): Unit = {
    worldBatch.dispose()
    hudBatch.dispose()
    gameView.dispose()
    physicsController.dispose()
    playScreen.dispose()
  }
}
