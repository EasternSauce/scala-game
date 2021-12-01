package com.easternsauce.screen

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.{GL20, OrthographicCamera}
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.{TiledMap, TiledMapTileLayer, TmxMapLoader}
import com.badlogic.gdx.math.{Polygon, Vector2}
import com.badlogic.gdx.physics.box2d._
import com.badlogic.gdx.utils.viewport.{FitViewport, Viewport}
import com.badlogic.gdx.{Gdx, Input, Screen}
import com.easternsauce.model.GameState
import com.easternsauce.model.creature.Creature
import com.easternsauce.util.{Constants, Direction, TerrainTile}
import com.easternsauce.view.GameView
import com.softwaremill.quicklens._

import scala.collection.mutable.ListBuffer

class PlayScreen(batch: SpriteBatch, var gameState: GameState, var gameUpdater: GameView) extends Screen {

  var b2DebugRenderer: Box2DDebugRenderer = new Box2DDebugRenderer()

  var world = new World(new Vector2(0, 0), true)

  val area1DataLocation = "assets/areas/area1"
  val mapLoader: TmxMapLoader = new TmxMapLoader()
  val mapScale = 4.0f
  val map: TiledMap = mapLoader.load(area1DataLocation + "/tile_map.tmx")
  protected val tiledMapRenderer: OrthogonalTiledMapRenderer =
    new OrthogonalTiledMapRenderer(map, mapScale / Constants.PPM)

  var widthInTiles: Int = _
  var heightInTiles: Int = _
  var tileWidth: Float = _
  var tileHeight: Float = _

  val terrainTiles: ListBuffer[TerrainTile] = ListBuffer()

  initPhysicalTerrain()

  var traversable: Array[Array[Boolean]] = _
  var traversableWithMargins: Array[Array[Boolean]] = _
  var flyover: Array[Array[Boolean]] = _

  val camera: OrthographicCamera = new OrthographicCamera()

  val viewport: Viewport =
    new FitViewport(
      Constants.ViewpointWorldWidth / Constants.PPM,
      Constants.ViewpointWorldHeight / Constants.PPM,
      camera
    )

  def updateCamera(player: Creature): Unit = {

    val camPosition = camera.position

    camPosition.x = (math.floor(player.params.posX * 1000) / 1000).toFloat
    camPosition.y = (math.floor(player.params.posY * 1000) / 1000).toFloat

    camera.update()

  }

  override def show(): Unit = {}

  def updateCreatures(delta: Float)(gameState: GameState): GameState = {

    val operation = (creature: Creature) => {
      val pos =
        if (gameUpdater.creatureRenderers.contains(creature.params.id))
          gameUpdater.creatureRenderers(creature.params.id).body.getWorldCenter
        else
          new Vector2(creature.params.posX, creature.params.posY)

      creature
        .modify(_.params.posX)
        .setTo(pos.x)
        .modify(_.params.posY)
        .setTo(pos.y)
        .update(delta)
    }

    gameState
      .modify(_.player)
      .using(operation)
      .modifyAll(_.nonPlayers.each)
      .using(operation)

  }

  def update(delta: Float): Unit = {

    world.step(Math.min(Gdx.graphics.getDeltaTime, 0.15f), 6, 2) //TODO: move to area class later

    // --- update com.easternsauce.model
    val performGameStateUpdates = (identity(_: GameState)) andThen
      processPlayerMovement andThen
      updateCreatures(delta)

    gameState = performGameStateUpdates(gameState)
    // ---

    // --- update com.easternsauce.view
    gameUpdater.update(gameState, world)
    // ---

    tiledMapRenderer.setView(camera)

    updateCamera(gameState.player)
  }

  private def processPlayerMovement(gameState: GameState): GameState = {
    val directionalSpeed: Float = {
      import Input.Keys._

      val sqrt2 = 1.4142135f
      val speed = 25f

      val directionalSpeed = List(W, S, A, D).map(Gdx.input.isKeyPressed(_)) match {
        case List(true, _, true, _) => speed / sqrt2
        case List(true, _, _, true) => speed / sqrt2
        case List(_, true, true, _) => speed / sqrt2
        case List(_, true, _, true) => speed / sqrt2
        case _                      => speed
      }
      directionalSpeed
    }

    val (vectorX, vectorY) = {
      import Input.Keys._

      val x: Float = List(A, D).map(Gdx.input.isKeyPressed(_)) match {
        case List(true, false) => -directionalSpeed
        case List(false, true) => directionalSpeed
        case _                 => 0
      }

      val y: Float = List(S, W).map(Gdx.input.isKeyPressed(_)) match {
        case List(true, false) => -directionalSpeed
        case List(false, true) => directionalSpeed
        case _                 => 0
      }

      (x, y)
    }

    val (facingDirection, isMoving) = {
      import Input.Keys._
      List(W, S, A, D).map(Gdx.input.isKeyPressed(_)) match {
        case List(true, _, _, _) => (Direction.Up, true)
        case List(_, true, _, _) => (Direction.Down, true)
        case List(_, _, true, _) => (Direction.Left, true)
        case List(_, _, _, true) => (Direction.Right, true)
        case _                   => (gameState.player.params.facingDirection, false)
      }

    }

    val wasMoving = gameState.player.params.isMoving
    val startMovingAction = (wasMoving, isMoving) match {
      case (false, true) =>
        player: Creature => {
          player.modify(_.params.animationTimer).using(_.restart())
        }
      case _ => player: Creature => player
    }

    val playerCreated = gameUpdater.creatureRenderers.contains(gameState.player.params.id)

    if (playerCreated)
      gameUpdater.creatureRenderers(gameState.player.params.id).body.setLinearVelocity(new Vector2(vectorX, vectorY))

    val pos =
      if (playerCreated)
        gameUpdater.creatureRenderers(gameState.player.params.id).body.getWorldCenter
      else
        new Vector2(gameState.player.params.posX, gameState.player.params.posY)

    gameState
      .modify(_.player.params.posX)
      .setTo(pos.x)
      .modify(_.player.params.posY)
      .setTo(pos.y)
      .modify(_.player.params.facingDirection)
      .setTo(facingDirection)
      .modify(_.player.params.isMoving)
      .setTo(isMoving)
      .modify(_.player)
      .using(startMovingAction)

  }

  override def render(delta: Float): Unit = {
    update(delta)

    batch.setProjectionMatrix(camera.combined)

    Gdx.gl.glClearColor(0, 0, 0, 1)

    val coverageBuffer = if (Gdx.graphics.getBufferFormat.coverageSampling) GL20.GL_COVERAGE_BUFFER_BIT_NV else 0
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | coverageBuffer)

    tiledMapRenderer.render(Array(0, 1))

    batch.begin()

    gameUpdater.render(gameState, batch)

    batch.end()

    tiledMapRenderer.render(Array(2, 3))

    //b2DebugRenderer.render(world, camera.combined)

  }

  def initPhysicalTerrain(): Unit = {
    val layer = map.getLayers.get(0).asInstanceOf[TiledMapTileLayer]

    widthInTiles = layer.getWidth
    heightInTiles = layer.getHeight

    tileWidth = layer.getTileWidth * mapScale / Constants.PPM
    tileHeight = layer.getTileHeight * mapScale / Constants.PPM

    traversable = Array.ofDim(heightInTiles, widthInTiles)
    traversableWithMargins = Array.ofDim(heightInTiles, widthInTiles)
    flyover = Array.ofDim(heightInTiles, widthInTiles)

    for {
      x <- 0 until widthInTiles
      y <- 0 until heightInTiles
    } traversable(y)(x) = true

    for {
      x <- 0 until widthInTiles
      y <- 0 until heightInTiles
    } traversableWithMargins(y)(x) = true

    for {
      x <- 0 until widthInTiles
      y <- 0 until heightInTiles
    } flyover(y)(x) = true

    createMapTerrain()
    createBorders()

  }

  private def createMapTerrain(): Unit = {
    def tileExists(x: Int, y: Int) = x >= 0 && x < widthInTiles && y >= 0 && y < heightInTiles

    for (layerNum <- 0 to 1) { // two layers
      val layer: TiledMapTileLayer =
        map.getLayers.get(layerNum).asInstanceOf[TiledMapTileLayer]

      for {
        x <- Seq.range(0, layer.getWidth)
        y <- Seq.range(0, layer.getHeight)
      } {
        val cell: TiledMapTileLayer.Cell = layer.getCell(x, y)

        if (cell != null) {
          val isTileTraversable: Boolean =
            cell.getTile.getProperties.get("traversable").asInstanceOf[Boolean]
          val isTileFlyover: Boolean =
            cell.getTile.getProperties.get("flyover").asInstanceOf[Boolean]

          if (!isTileTraversable) {
            traversable(y)(x) = false

            traversableWithMargins(y)(x) = false

            List((0, 1), (1, 0), (-1, 0), (0, -1), (1, 1), (-1, 1), (-1, -1), (1, -1))
              .filter(pair => tileExists(x + pair._1, y + pair._2))
              .foreach(pair => traversableWithMargins(y + pair._2)(x + pair._1) = false)
          }

          if (!isTileFlyover) flyover(y)(x) = false
        }

      }

      for {
        x <- 0 until widthInTiles
        y <- 0 until heightInTiles
      } {

        if (!traversable(y)(x)) {
          val bodyDef = new BodyDef()
          bodyDef.`type` = BodyDef.BodyType.StaticBody
          bodyDef.position
            .set(x * tileWidth + tileWidth / 2, y * tileHeight + tileHeight / 2)

          val body: Body = world.createBody(bodyDef)

          val polygon = new Polygon(
            Array(
              x * tileWidth,
              y * tileHeight,
              x * tileWidth + tileWidth,
              y * tileHeight,
              x * tileWidth + tileWidth,
              y * tileHeight + tileHeight,
              x * tileWidth,
              y * tileHeight + tileHeight
            )
          )

          val tile: TerrainTile = TerrainTile((layerNum, x, y), body, flyover(y)(x), polygon)

          terrainTiles += tile

          body.setUserData(tile)

          val shape: PolygonShape = new PolygonShape()

          shape.setAsBox(tileWidth / 2, tileHeight / 2)

          val fixtureDef: FixtureDef = new FixtureDef

          fixtureDef.shape = shape

          body.createFixture(fixtureDef)
        }
      }

    }
  }

  private def createBorders(): Unit = {

    for { x <- Seq.range(0, widthInTiles) } {
      createBorderTile(x, -1)
      createBorderTile(x, heightInTiles)
    }

    for { y <- Seq.range(0, heightInTiles) } {
      createBorderTile(-1, y)
      createBorderTile(widthInTiles, y)
    }
  }

  private def createBorderTile(x: Int, y: Int) = {
    val bodyDef = new BodyDef()
    bodyDef.`type` = BodyDef.BodyType.StaticBody
    val tileCenter = getTileCenter(x, y)
    bodyDef.position.set(tileCenter.x, tileCenter.y)

    val body: Body = world.createBody(bodyDef)

    body.setUserData(this)

    val shape: PolygonShape = new PolygonShape()

    shape.setAsBox(tileWidth / 2, tileHeight / 2)

    val fixtureDef: FixtureDef = new FixtureDef

    fixtureDef.shape = shape

    body.createFixture(fixtureDef)
  }

  def getTileCenter(x: Int, y: Int): Vector2 = {
    new Vector2(x * tileWidth + tileWidth / 2, y * tileHeight + tileHeight / 2)
  }

  def getClosestTile(x: Float, y: Float): Vector2 = {
    new Vector2(x / tileWidth, y / tileHeight)
  }

  override def resize(width: Int, height: Int): Unit = {
    viewport.update(width, height)
  }

  override def pause(): Unit = {}

  override def resume(): Unit = {}

  override def hide(): Unit = {}

  override def dispose(): Unit = {
    batch.dispose()
  }
}
