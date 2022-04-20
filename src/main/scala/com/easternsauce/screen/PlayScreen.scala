package com.easternsauce.screen

import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.graphics.{GL20, OrthographicCamera}
import com.badlogic.gdx.math.{Vector2, Vector3}
import com.badlogic.gdx.physics.box2d._
import com.badlogic.gdx.utils.viewport.{FitViewport, Viewport}
import com.badlogic.gdx.{Gdx, Input, Screen}
import com.easternsauce.event.{AreaChangeEvent, CollisionEvent}
import com.easternsauce.model.GameState
import com.easternsauce.model.creature.Creature
import com.easternsauce.util.{Constants, RendererBatch, Vector2Wrapper}
import com.easternsauce.view.physics.PhysicsController
import com.easternsauce.view.renderer.RendererController
import com.softwaremill.quicklens._
import io.circe.syntax.EncoderOps

import java.io.{File, PrintWriter}
import scala.collection.mutable.ListBuffer
import scala.util.chaining._

class PlayScreen(
  worldBatch: RendererBatch,
  hudBatch: RendererBatch,
  state: GameState,
  view: RendererController,
  physics: PhysicsController
) extends Screen {
  val b2DebugRenderer: Box2DDebugRenderer = new Box2DDebugRenderer()

  val debugRenderEnabled = true

  var gameState: GameState = state
  var gameView: RendererController = view
  var physicsController: PhysicsController = physics

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

  var justStarted = true

  def updateCamera(player: Creature): Unit = {

    val camPosition = worldCamera.position

    camPosition.x = (math.floor(player.params.posX * 100) / 100).toFloat
    camPosition.y = (math.floor(player.params.posY * 100) / 100).toFloat

    worldCamera.update()

  }

  override def show(): Unit = {}

  def updateCreatures(physicsController: PhysicsController, delta: Float)(gameState: GameState): GameState = {

    val updateCreature = (creature: Creature) => {
      val pos =
        if (physicsController.entityBodies.contains(creature.params.id))
          physicsController.entityBodies(creature.params.id).pos
        else
          new Vector2(creature.params.posX, creature.params.posY)

      creature
        .setPosition(pos.x, pos.y)
        .update(delta)
        .pipe(
          creature => if (creature.isControlledAutomatically) creature.updateAutomaticControls(gameState) else creature
        )
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
          case (acc, (abilityId, creatureId)) =>
            acc.updateCreatureAbility(physicsController, creatureId, abilityId, delta)
        })
      })

  }

  def update(delta: Float): Unit = {

    val currentArea = gameView.areaRenderers(gameState.currentAreaId)
    val currentTerrain = physicsController.terrains(gameState.currentAreaId)

    currentTerrain.step()

    // --- update model (can update based on player input or physical world state)
    gameState = gameState
      .pipe(_.clearEventsQueue())
      .pipe(processPlayerInput)
      .pipe(processInventoryActions)
      .pipe(updateCreatures(physicsController, delta))
      .pipe(_.processCreatureAreaChanges(areaChangeQueue))
      .pipe(_.processCollisions(collisionQueue))
    // ---

    // --- update libGDX view
    gameView.update(gameState)
    // ---

    // --- update physics
    physicsController.update(gameState, areaChangeQueue)
    //

    areaChangeQueue.clear()
    collisionQueue.clear()

    currentArea.setView(worldCamera)

    updateCamera(gameState.player)
  }

  private def processPlayerInput(gameState: GameState): GameState = {
    // TODO: temporarily simulate creature changing areas on SPACE
    if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
      if (gameState.currentAreaId == "area1") {
        areaChangeQueue.prepend(AreaChangeEvent("player", "area1", "area2"))
      } else {
        areaChangeQueue.prepend(AreaChangeEvent("player", "area2", "area1"))
      }
    }

    val handleInventoryOpenClose: GameState => GameState = gameState => {
      val inventoryOpen = gameState.inventoryState.inventoryOpen
      if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
        if (!inventoryOpen) {
          gameState.modify(_.inventoryState.inventoryOpen).setTo(true)
        } else { gameState.modify(_.inventoryState.inventoryOpen).setTo(false) }

      } else gameState
    }

    val handleLeftClickInput: GameState => GameState =
      if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
        val mouseX = Gdx.input.getX
        val mouseY = Gdx.input.getY

        val centerX = Gdx.graphics.getWidth / 2f
        val centerY = Gdx.graphics.getHeight / 2f

        val facingVector = Vector2Wrapper(mouseX - centerX, (Gdx.graphics.getHeight - mouseY) - centerY).normal

        val modification: GameState => GameState =
          _.modifyGameStateCreature("player")(
            _.modify(_.params.actionDirVector).setTo(facingVector).performAbility("defaultAbility")
          )
        modification
      } else identity

    val handleMovement: GameState => GameState = { gameState =>
      val (movementVectorX, movementVectorY) = {
        import Input.Keys._

        val x: Float = List(A, D).map(Gdx.input.isKeyPressed(_)) match {
          case List(true, false) => -1
          case List(false, true) => 1
          case _                 => 0
        }

        val y: Float = List(S, W).map(Gdx.input.isKeyPressed(_)) match {
          case List(true, false) => -1
          case List(false, true) => 1
          case _                 => 0
        }

        (x, y) match {
          case (0, 0) => (gameState.player.params.movingDir.x, gameState.player.params.movingDir.y)
          case _      => (x, y)
        }
      }

      val ableToMove = !gameState.player.isEffectActive("stagger") && gameState.player.isAlive

      if (ableToMove) gameState.modify(_.player).using { player =>
        val isMoving = {
          import Input.Keys._
          List(W, S, A, D).map(Gdx.input.isKeyPressed(_)) match {
            case List(true, _, _, _) => true
            case List(_, true, _, _) => true
            case List(_, _, true, _) => true
            case List(_, _, _, true) => true
            case _                   => false
          }

        }

        val wasMoving = gameState.player.isMoving

        player
          .modify(_.params.movingDir)
          .setTo(Vector2Wrapper(movementVectorX, movementVectorY).normal)
          .pipe((wasMoving, isMoving) match {

            case (false, true) =>
              player: Creature => player.startMoving()

            case (true, false) =>
              player: Creature => player.stopMoving()
            case _ => player: Creature => player
          })

      }
      else gameState
    }

    gameState
      .pipe(handleMovement)
      .pipe(handleLeftClickInput)
      .pipe(handleInventoryOpenClose)

  }

  def processInventoryActions(gameState: GameState): GameState = {

    if (Gdx.input.isButtonJustPressed(Buttons.LEFT)) {
      gameState.pipe(
        gameState =>
          if (gameState.inventoryState.inventoryOpen) gameState.moveItemClick(mousePosWindowScaled) else gameState
      )
    } else gameState
  }

  override def render(delta: Float): Unit = {
    update(delta)

    if (justStarted) { // delay rendering by one frame
      justStarted = false
      return
    }

    worldBatch.spriteBatch.setProjectionMatrix(worldCamera.combined)
    hudBatch.spriteBatch.setProjectionMatrix(hudCamera.combined)

    Gdx.gl.glClearColor(0, 0, 0, 1)

    val coverageBuffer = if (Gdx.graphics.getBufferFormat.coverageSampling) GL20.GL_COVERAGE_BUFFER_BIT_NV else 0
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | coverageBuffer)

    val currentArea = gameView.areaRenderers(gameState.currentAreaId)
    val currentTerrain = physicsController.terrains(gameState.currentAreaId)

    currentArea.render(Array(0, 1))

    worldBatch.begin()

    gameView.renderAreaGates(gameState, worldBatch)

    gameView.renderEntities(gameState, worldBatch)

    worldBatch.end()

    currentArea.render(Array(2, 3))

    worldBatch.begin()

    gameView.renderAbilities(gameState, worldBatch)

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

    val saveFilePath = "saves"
    new File(saveFilePath).mkdir()

    val writer = new PrintWriter(new File(saveFilePath + "/savefile.txt"))

    import com.easternsauce.json.JsonCodecs._

    writer.write(gameState.asJson.toString())

    writer.close()
  }

  def mousePosWindowScaled: Vector2 = {
    val v = new Vector3(Gdx.input.getX.toFloat, Gdx.input.getY.toFloat, 0f)
    hudCamera.unproject(v)
    new Vector2(v.x, v.y)
  }
}
