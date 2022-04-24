package com.easternsauce.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.{TiledMap, TmxMapLoader}
import com.easternsauce.event.PhysicsEvent
import com.easternsauce.json.JsonCodecs._
import com.easternsauce.model.GameState
import com.easternsauce.model.area.{Area, EnemySpawnPoint}
import com.easternsauce.model.creature.{CreatureParams, Player, Serpent, Skeleton}
import com.easternsauce.model.event.AreaChangeEvent
import com.easternsauce.screen.PlayScreen
import com.easternsauce.system.Assets
import com.easternsauce.util.RendererBatch
import com.easternsauce.view.physics.PhysicsController
import com.easternsauce.view.physics.terrain.{AreaGatePair, Terrain}
import com.easternsauce.view.renderer
import com.easternsauce.view.renderer.RendererController
import io.circe.parser.decode

import java.io.FileNotFoundException
import scala.collection.mutable.ListBuffer

class MyGdxGame extends Game {

  private var worldBatch: RendererBatch = _
  private var hudBatch: RendererBatch = _

  var atlas: TextureAtlas = _

  var gameView: RendererController = _

  private val mapScale = 4.0f

  val mapsToLoad =
    Map("area1" -> "assets/areas/area1", "area2" -> "assets/areas/area2", "area3" -> "assets/areas/area3")

  val areaGates: List[AreaGatePair] = List( // TODO: load this from file?
    AreaGatePair("area1", 199.5f, 15f, "area3", 17f, 2.5f),
    AreaGatePair("area1", 2f, 63f, "area2", 58f, 9f)
  )

  val mapLoader: TmxMapLoader = new TmxMapLoader()

  var maps: Map[String, TiledMap] = _

  var physicsController: PhysicsController = _

  var playScreen: PlayScreen = _

  var areaChangeQueue: ListBuffer[AreaChangeEvent] = ListBuffer()
  var collisionQueue: ListBuffer[PhysicsEvent] = ListBuffer()

  override def create(): Unit = {
    Assets.loadAssets()

    worldBatch = RendererBatch()
    hudBatch = RendererBatch()

    atlas = new TextureAtlas("assets/atlas/packed_atlas.atlas")

    val player: Player = Player(
      CreatureParams(
        id = "player",
        posX = 50,
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
        posX = 20,
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

    val areas = maps.map {
      case (key, _) => (key, Area(areaId = key, spawnPoints = loadEnemySpawns("assets/areas/" + key)))
    }

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

    val terrains: Map[String, Terrain] = maps.map { case (areaId, map) => areaId -> Terrain(map, mapScale) }

    areaGates.foreach(_.init(terrains))

    physicsController = PhysicsController(terrains, areaGates)

    playScreen = new PlayScreen(worldBatch, hudBatch, gameState, gameView, physicsController, collisionQueue)

    gameView.init(gameState, maps, mapScale, areaGates)
    physicsController.init(gameState, collisionQueue)

    setScreen(playScreen)
  }

  override def dispose(): Unit = {
    worldBatch.dispose()
    hudBatch.dispose()
    gameView.dispose()
    physicsController.dispose()
    playScreen.dispose()
  }

  def loadEnemySpawns(areaFilesLocation: String): List[EnemySpawnPoint] = {
    val source = scala.io.Source.fromFile(areaFilesLocation + "/enemy_spawns.json")
    val lines =
      try source.mkString
      finally source.close()

    val decoded = decode[List[EnemySpawnPoint]](lines)

    decoded.getOrElse(throw new RuntimeException("failed to decode spawns file"))
  }
}
