package com.easternsauce.screen

import com.badlogic.gdx.graphics.{GL20, OrthographicCamera}
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d._
import com.badlogic.gdx.utils.viewport.{FitViewport, Viewport}
import com.badlogic.gdx.{Gdx, Input, Screen}
import com.easternsauce.model.GameState
import com.easternsauce.model.creature.Creature
import com.easternsauce.physics.PhysicsController
import com.easternsauce.util.{Constants, Direction, RendererBatch}
import com.easternsauce.view.GameView
import com.softwaremill.quicklens._

class PlayScreen(
  batch: RendererBatch,
  var gameState: GameState,
  var gameView: GameView,
  var physicsController: PhysicsController
) extends Screen {

  var b2DebugRenderer: Box2DDebugRenderer = new Box2DDebugRenderer()

  val debugRenderEnabled = false

  val camera: OrthographicCamera = new OrthographicCamera()

  val viewport: Viewport =
    new FitViewport(
      Constants.ViewpointWorldWidth / Constants.PPM,
      Constants.ViewpointWorldHeight / Constants.PPM,
      camera
    )

  var areaChangeQueue: List[(String, String, String)] = List()

  def updateCamera(player: Creature): Unit = {

    val camPosition = camera.position

    camPosition.x = (math.floor(player.params.posX * 100) / 100).toFloat
    camPosition.y = (math.floor(player.params.posY * 100) / 100).toFloat

    camera.update()

  }

  override def show(): Unit = {}

  def updateCreatures(physicsController: PhysicsController, delta: Float)(gameState: GameState): GameState = {

    def physicsPos(physicsController: PhysicsController, creature: Creature): Vector2 =
      if (physicsController.entityBodies.contains(creature.params.id))
        physicsController.entityBodies(creature.params.id).pos
      else
        new Vector2(creature.params.posX, creature.params.posY)

    val updateCreature = (creature: Creature) => {
      val pos = physicsPos(physicsController, creature)

      creature
        .updatePosition(pos.x, pos.y)
        .update(delta)
    }

    gameState
      .modify(_.player)
      .using(updateCreature)
      .modify(_.nonPlayers.each)
      .using(updateCreature)

  }

  def update(delta: Float): Unit = {

    val currentArea = gameView.areaRenderers(gameState.currentAreaId)
    val currentTerrain = physicsController.terrain(gameState.currentAreaId)

    currentTerrain.step()

    // TODO: creature changing area logic goes here
    // ...
    // TODO: temporarily simulate creature changing areas on SPACE
    if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
      if (gameState.currentAreaId == "area1") {
        areaChangeQueue = ("player", "area1", "area2") :: areaChangeQueue
      } else {
        areaChangeQueue = ("player", "area2", "area1") :: areaChangeQueue
      }
    }

    // --- update model (can update based on player input or physical world state)
    val performGameStateUpdates = (identity(_: GameState)) andThen
      processPlayerMovement andThen
      updateCreatures(physicsController, delta) andThen
      (_.processCreatureAreaChanges(areaChangeQueue))

    gameState = performGameStateUpdates(gameState)
    // ---

    // --- update libGDX view
    gameView.update(gameState, currentTerrain.world)
    // ---

    // --- update physics
    physicsController.processCreatureAreaChanges(gameState, areaChangeQueue)
    //

    areaChangeQueue = List()

    currentArea.setView(camera)

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

    val playerBodyCreated = physicsController.entityBodies.contains(gameState.player.params.id)

    if (playerBodyCreated)
      physicsController.entityBodies(gameState.player.params.id).setVelocity(new Vector2(vectorX, vectorY))

    val pos =
      if (playerBodyCreated)
        physicsController.entityBodies(gameState.player.params.id).pos
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

    batch.spriteBatch.setProjectionMatrix(camera.combined)

    Gdx.gl.glClearColor(0, 0, 0, 1)

    val coverageBuffer = if (Gdx.graphics.getBufferFormat.coverageSampling) GL20.GL_COVERAGE_BUFFER_BIT_NV else 0
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | coverageBuffer)

    val currentArea = gameView.areaRenderers(gameState.currentAreaId)
    val currentTerrain = physicsController.terrain(gameState.currentAreaId)

    currentArea.render(Array(0, 1))

    batch.begin()

    gameView.render(gameState, batch)

    batch.end()

    currentArea.render(Array(2, 3))

    if (debugRenderEnabled) b2DebugRenderer.render(currentTerrain.world, camera.combined)

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
