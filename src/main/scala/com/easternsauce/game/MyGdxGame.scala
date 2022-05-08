package com.easternsauce.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.{TiledMap, TmxMapLoader}
import com.easternsauce.event.PhysicsEvent
import com.easternsauce.json.JsonCodecs._
import com.easternsauce.model.GameState
import com.easternsauce.model.area.{Area, EnemySpawnPoint}
import com.easternsauce.model.creature.{CreatureParams, Player}
import com.easternsauce.screen.PlayScreen
import com.easternsauce.system.Assets
import com.easternsauce.util.RendererBatch
import com.easternsauce.view.physics.PhysicsController
import com.easternsauce.view.physics.terrain.{AreaGateBody, Terrain}
import com.easternsauce.view.renderer
import com.easternsauce.view.renderer.RendererController
import io.circe.parser.decode

import java.io.FileNotFoundException
import scala.collection.mutable.ListBuffer
import scala.util.chaining.scalaUtilChainingOps

class MyGdxGame extends Game {

  private var worldBatch: RendererBatch = _
  private var hudBatch: RendererBatch = _

  var atlas: TextureAtlas = _

  var gameView: RendererController = _

  private val mapScale = 4.0f

  val mapsToLoad =
    Map("area1" -> "assets/areas/area1", "area2" -> "assets/areas/area2", "area3" -> "assets/areas/area3")

  val areaGates: List[AreaGateBody] = List( // TODO: load this from file?
    AreaGateBody("area1", 199.5f, 15f, "area3", 17f, 2.5f),
    AreaGateBody("area1", 2f, 63f, "area2", 58f, 9f)
  )

  val mapLoader: TmxMapLoader = new TmxMapLoader()

  var maps: Map[String, TiledMap] = _

  var physicsController: PhysicsController = _

  var playScreen: PlayScreen = _

  var physicsEventQueue: ListBuffer[PhysicsEvent] = ListBuffer()

  override def create(): Unit = {
    Assets.loadAssets()

    worldBatch = RendererBatch()
    hudBatch = RendererBatch()

    atlas = new TextureAtlas("assets/atlas/packed_atlas.atlas")

    val player: Player = Player(
      CreatureParams( // TODO ?
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

    maps = mapsToLoad.map {
      case (areaId, directory) => areaId -> mapLoader.load(directory + "/tile_map.tmx")
    }

    val areas = maps
      .map {
        case (key, _) => (key, Area(areaId = key, spawnPoints = loadEnemySpawns("assets/areas/" + key)))
      }
//      .pipe(
//        areas =>
//          areas
//            .modify(_.at("area1").params.lootPiles)
//            .setTo(Map("lootPile1" -> LootPile(54, 15), "lootPile2" -> LootPile(54, 16)))
//      )
//      .pipe(
//        areas =>
//          areas
//            .modify(_.at("area2").params.lootPiles)
//            .setTo(
//              Map("lootPile3" -> LootPile(30, 30), "lootPile4" -> LootPile(30, 30), "lootPile5" -> LootPile(30, 30))
//            )
//      )
//      .pipe(areas => areas.modify(_.at("area3").params.lootPiles).setTo(Map("lootPile6" -> LootPile(30, 30))))

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
            currentPlayerId = player.params.id,
            creatures = Map(player.params.id -> player.init()), // TODO: init elsewhere?
            currentAreaId = "area1",
            areas = areas
          ).pipe(gameState => gameState.resetArea(gameState.currentAreaId))

      }

    gameView = renderer.RendererController(atlas)

    val terrains: Map[String, Terrain] = maps.map { case (areaId, map) => areaId -> Terrain(map, mapScale) }

    areaGates.foreach(_.init(terrains))

    physicsController = PhysicsController(terrains, areaGates)

    playScreen = new PlayScreen(worldBatch, hudBatch, gameState, gameView, physicsController, physicsEventQueue)

    gameView.init(gameState, maps, mapScale, areaGates)
    physicsController.init(gameState, physicsEventQueue)

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
