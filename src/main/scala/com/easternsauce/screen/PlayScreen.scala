package com.easternsauce.screen

import com.badlogic.gdx.graphics.{GL20, OrthographicCamera}
import com.badlogic.gdx.math.{Vector2, Vector3}
import com.badlogic.gdx.physics.box2d._
import com.badlogic.gdx.utils.viewport.{FitViewport, Viewport}
import com.badlogic.gdx.{Gdx, Input, Screen}
import com.easternsauce.box2d_physics.PhysicsController
import com.easternsauce.event.{AreaChangeEvent, CollisionEvent}
import com.easternsauce.model.GameState
import com.easternsauce.model.creature.Creature
import com.easternsauce.util.{Constants, Direction, RendererBatch}
import com.easternsauce.view.GameView
import com.softwaremill.quicklens._

import scala.collection.mutable.ListBuffer
import scala.util.chaining._

class PlayScreen(
  worldBatch: RendererBatch,
  hudBatch: RendererBatch,
  var gameState: GameState,
  var gameView: GameView,
  var physicsController: PhysicsController
) extends Screen {

  val b2DebugRenderer: Box2DDebugRenderer = new Box2DDebugRenderer()

  val debugRenderEnabled = true

  val worldCamera: OrthographicCamera = new OrthographicCamera()
  val hudCamera: OrthographicCamera = {
    val cam = new OrthographicCamera()
    cam.position.set(Constants.WindowWidth / 2f, Constants.WindowHeight / 2f, 0)
    cam
  }

  val worldViewport: Viewport =
    new FitViewport(
      Constants.ViewpointWorldWidth / Constants.PPM,
      Constants.ViewpointWorldHeight / Constants.PPM,
      worldCamera
    )

  val hudViewport: Viewport =
    new FitViewport(Constants.WindowWidth.toFloat, Constants.WindowHeight.toFloat, hudCamera)

  var areaChangeQueue: ListBuffer[AreaChangeEvent] = ListBuffer()
  var collisionQueue: ListBuffer[CollisionEvent] = ListBuffer()

  physicsController.setCollisionQueue(collisionQueue)

  def updateCamera(player: Creature): Unit = {

    val camPosition = worldCamera.position

    camPosition.x = (math.floor(player.params.posX * 100) / 100).toFloat
    camPosition.y = (math.floor(player.params.posY * 100) / 100).toFloat

    worldCamera.update()

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
        .setPosition(pos.x, pos.y)
        .update(delta)
    }

    gameState // TODO: dont update creatures outside the current area
      .modify(_.player)
      .using(updateCreature)
      .modify(_.nonPlayers.each)
      .using(updateCreature)
      .pipe(gameState => {
        val creatureAbilityPairs =
          gameState.creatures.values.flatMap(creature => creature.params.abilities.keys.map((_, creature.params.id)))
        creatureAbilityPairs.foldLeft(gameState)({
          case (acc, (abilityId, creatureId)) => acc.updateCreatureAbility(creatureId, abilityId, delta)
        })
      })

  }

  def update(delta: Float): Unit = {

    val currentArea = gameView.areaRenderers(gameState.currentAreaId)
    val currentTerrain = physicsController.terrain(gameState.currentAreaId)

    currentTerrain.step()

    // --- update model (can update based on player input or physical world state)
    gameState = gameState
      .pipe(_.clearEventsQueue())
      .pipe(processPlayerMovement)
      .pipe(updateCreatures(physicsController, delta))
      .pipe(_.processCreatureAreaChanges(areaChangeQueue))
      .pipe(_.processCollisions(collisionQueue))
    // ---

    // --- update libGDX view
    gameView.update(gameState, currentTerrain.world)
    // ---

    // --- update physics
    physicsController.update(gameState, areaChangeQueue)
    //

    areaChangeQueue.clear()
    collisionQueue.clear()

    currentArea.setView(worldCamera)

    updateCamera(gameState.player)
  }

  private def processPlayerMovement(gameState: GameState): GameState = {
    // TODO: temporarily simulate creature changing areas on SPACE
    if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
      if (gameState.currentAreaId == "area1") {
        areaChangeQueue.prepend(AreaChangeEvent("player", "area1", "area2"))
      } else {
        areaChangeQueue.prepend(AreaChangeEvent("player", "area2", "area1"))
      }
    }

    val leftClickInput: GameState => GameState =
      if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
        val mouseX = Gdx.input.getX
        val mouseY = Gdx.input.getY

        val centerX = Gdx.graphics.getWidth / 2f
        val centerY = Gdx.graphics.getHeight / 2f

        val facingVector = new Vector2(mouseX - centerX, (Gdx.graphics.getHeight - mouseY) - centerY).nor()

        val modification: GameState => GameState =
          _.modifyGameStateCreature("player")(_.modify(_.params.dirVector).setTo(facingVector))
            .performAbility("player", "regularAttack")
        modification
      } else identity

    val directionalSpeed: Float = {
      import Input.Keys._

      val sqrt2 = 1.4142135f
      val speed = 25f

      List(W, S, A, D).map(Gdx.input.isKeyPressed(_)) match {
        case List(true, _, true, _) => speed / sqrt2
        case List(true, _, _, true) => speed / sqrt2
        case List(_, true, true, _) => speed / sqrt2
        case List(_, true, _, true) => speed / sqrt2
        case _                      => speed
      }
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
      .pipe(leftClickInput)

  }

  override def render(delta: Float): Unit = {
    update(delta)

    worldBatch.spriteBatch.setProjectionMatrix(worldCamera.combined)
    hudBatch.spriteBatch.setProjectionMatrix(hudCamera.combined)

    Gdx.gl.glClearColor(0, 0, 0, 1)

    val coverageBuffer = if (Gdx.graphics.getBufferFormat.coverageSampling) GL20.GL_COVERAGE_BUFFER_BIT_NV else 0
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | coverageBuffer)

    val currentArea = gameView.areaRenderers(gameState.currentAreaId)
    val currentTerrain = physicsController.terrain(gameState.currentAreaId)

    currentArea.render(Array(0, 1))

    worldBatch.begin()

    gameView.renderEntities(gameState, worldBatch)

    worldBatch.end()

    currentArea.render(Array(2, 3))

    worldBatch.begin()

    gameView.renderAbilties(gameState, worldBatch)

    worldBatch.end()

    hudBatch.begin()

    gameView.renderHud(gameState, hudBatch, mousePosWindowScaled)

    hudBatch.end()

    if (debugRenderEnabled) b2DebugRenderer.render(currentTerrain.world, worldCamera.combined)

  }

  override def resize(width: Int, height: Int): Unit = {
    worldViewport.update(width, height)
    hudViewport.update(width, height)

  }

  override def pause(): Unit = {}

  override def resume(): Unit = {}

  override def hide(): Unit = {}

  override def dispose(): Unit = {
    worldBatch.dispose()
  }

  def mousePosWindowScaled: Vector2 = {
    val v = new Vector3(Gdx.input.getX.toFloat, Gdx.input.getY.toFloat, 0f)
    hudCamera.unproject(v)
    new Vector2(v.x, v.y)
  }
}
