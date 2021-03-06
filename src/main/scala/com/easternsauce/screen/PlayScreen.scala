package com.easternsauce.screen

import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.graphics.{Color, GL20, OrthographicCamera}
import com.badlogic.gdx.math.{Vector2, Vector3}
import com.badlogic.gdx.physics.box2d._
import com.badlogic.gdx.utils.viewport.{FitViewport, Viewport}
import com.badlogic.gdx.{Gdx, Input, Screen}
import com.easternsauce.event.PhysicsEvent
import com.easternsauce.helper.LootPickupMenuHelper
import com.easternsauce.json.JsonCodecs
import com.easternsauce.model.GameState
import com.easternsauce.model.creature.Creature
import com.easternsauce.model.util.EnhancedChainingSyntax.enhancedScalaUtilChainingOps
import com.easternsauce.system.Assets
import com.easternsauce.util.{Constants, RendererBatch, Vec2}
import com.easternsauce.view.physics.PhysicsController
import com.easternsauce.view.renderer.RendererController
import com.easternsauce.view.sound.SoundManager
import com.softwaremill.quicklens._
import io.circe.syntax.EncoderOps

import java.io.{File, PrintWriter}
import scala.collection.mutable.ListBuffer

class PlayScreen(
  val worldBatch: RendererBatch,
  val hudBatch: RendererBatch,
  val state: GameState,
  var gameRenderer: RendererController,
  var physicsController: PhysicsController,
  var soundManager: SoundManager,
  var physicsEventQueue: ListBuffer[PhysicsEvent]
) extends Screen {
  import com.easternsauce.system.Assets.bitmapFontToEnrichedBitmapFont

  val b2DebugRenderer: Box2DDebugRenderer = new Box2DDebugRenderer()

  val debugRenderEnabled = true

  val worldCamera: OrthographicCamera = new OrthographicCamera()
  val hudCamera: OrthographicCamera = {
    val cam = new OrthographicCamera()
    cam.position.set(Constants.WindowWidth / 2f, Constants.WindowHeight / 2f, 0)
    cam
  }

  var gameState: GameState = state

  val worldViewport: Viewport =
    new FitViewport(
      Constants.ViewpointWorldWidth / Constants.PPM,
      Constants.ViewpointWorldHeight / Constants.PPM,
      worldCamera
    )

  val hudViewport: Viewport =
    new FitViewport(Constants.WindowWidth.toFloat, Constants.WindowHeight.toFloat, hudCamera)

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

      if (creature.params.areaId == gameState.currentAreaId) {
        creature
          .setPosition(pos.x, pos.y)
          .update(delta)
//          .pipe(
//            creature =>
//              if (creature.isControlledAutomatically) creature.updateAutomaticControls(gameState) else creature
//          )
      } else creature
    }

    gameState // TODO: dont update creatures outside the current area
      .modify(_.creatures.each)
      .using(updateCreature)
      .pipe(
        gameState =>
          gameState.creatures.foldLeft(gameState) {
            case (gameState, (_, creature)) =>
              if (creature.isControlledAutomatically) creature.updateAutomaticControls(gameState) else gameState
          }
      )
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

    val currentArea = gameRenderer.areaRenderers(gameState.currentAreaId)
    val currentTerrain = physicsController.terrains(gameState.currentAreaId)

    currentTerrain.step()

    // --- update model (can update based on player input or physical world state)
    gameState = gameState
      .pipe(_.clearEventQueue())
      .pipe(processPlayerInput)
      .pipe(updateCreatures(physicsController, delta))
      .pipe(_.processPhysicsEventQueue(physicsEventQueue.toList))
      .pipe(_.processCreatureAreaChanges())
      .pipe(_.processPathfinding(physicsController))

    // ---

    // mark all physics engine events as consumed
    physicsEventQueue.clear()

    // --- update libGDX renderer
    gameRenderer.update(gameState)
    // ---

    // --- update physics
    physicsController.update(gameState)

    // --- update sound manager
    soundManager.update(gameState)

    currentArea.setView(worldCamera)

    updateCamera(gameState.player)
  }

  private def processPlayerInput(gameState: GameState): GameState = {
    // TODO: temporarily simulate creature changing areas on SPACE
//    if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
//      if (gameState.currentAreaId == "area1") {
//        areaChangeQueue.prepend(AreaChangeEvent("player", "area1", "area2"))
//      } else {
//        areaChangeQueue.prepend(AreaChangeEvent("player", "area2", "area1"))
//      }
//    }

    val handleDebugButton: GameState => GameState = gameState => {
      if (Gdx.input.isKeyJustPressed(Input.Keys.F6)) gameState.resetArea("area1")
      else gameState
    }

    val handleCheatsButton: GameState => GameState = gameState => {
      if (Gdx.input.isKeyJustPressed(Input.Keys.F12))
        gameState.modifyGameStateCreature(gameState.currentPlayerId)(_.modify(_.params.life).setTo(Float.MaxValue))
      else gameState
    }

    val handleInventoryOpenClose: GameState => GameState = gameState => {
      val inventoryOpen = gameState.inventoryWindow.isOpen
      if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
        if (!inventoryOpen) {
          gameState.modify(_.inventoryWindow.isOpen).setTo(true)
        } else { gameState.modify(_.inventoryWindow.isOpen).setTo(false) }

      } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
        if (inventoryOpen) {
          gameState.modify(_.inventoryWindow.isOpen).setTo(false)
        } else gameState

      } else gameState
    }

    def processInventoryWindowActions: GameState => GameState =
      gameState => {

        if (Gdx.input.isButtonJustPressed(Buttons.LEFT)) {
          gameState.pipeIf(gameState.inventoryWindow.isOpen)(_.moveItemClick(mousePosWindowScaled))
        } else gameState
      }

    def processLootPileMenuActions: GameState => GameState =
      gameState => {

        if (Gdx.input.isButtonJustPressed(Buttons.LEFT)) {
          gameState.pipeIf(gameState.lootPilePickupMenu.isOpen && !gameState.inventoryWindow.isOpen)(
            _.lootPickupMenuClick(mousePosWindowScaled)
          )
        } else gameState
      }

    val handleAttackCommand: GameState => GameState = {

      if (
        Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !LootPickupMenuHelper.inLootPickupMenu(
          gameState,
          mousePosWindowScaled
        )
      ) {
        val mouseX = Gdx.input.getX
        val mouseY = Gdx.input.getY

        val centerX = Gdx.graphics.getWidth / 2f
        val centerY = Gdx.graphics.getHeight / 2f

        val facingVector = Vec2(mouseX - centerX, (Gdx.graphics.getHeight - mouseY) - centerY).normal

        val modification: GameState => GameState =
          gameState =>
            gameState
              .creatures(gameState.currentPlayerId)
              .attack(gameState, facingVector)
        modification
      } else identity
    }

    val handleDashCommand: GameState => GameState = {

      if (
        Gdx.input
          .isKeyJustPressed(Input.Keys.SPACE) && !LootPickupMenuHelper.inLootPickupMenu(gameState, mousePosWindowScaled)
      ) {
        val mouseX = Gdx.input.getX
        val mouseY = Gdx.input.getY

        val centerX = Gdx.graphics.getWidth / 2f
        val centerY = Gdx.graphics.getHeight / 2f

        val facingVector = Vec2(mouseX - centerX, (Gdx.graphics.getHeight - mouseY) - centerY).normal

        //    gameState
        //      .modifyGameStateCreature(creatureId)(_.modify(_.params.actionDirVector).setTo(dir))
        //      .pipe(gameState => gameState.performAbility(creatureId, defaultAbilityId))

        val modification: GameState => GameState =
          gameState =>
            gameState
              .modifyGameStateCreature(gameState.currentPlayerId)(
                _.modify(_.params.actionDirVector).setTo(facingVector)
              )
              .pipe(gameState => gameState.creatures(gameState.currentPlayerId).performAbility(gameState, "dash"))

        modification
      } else identity
    }

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

      val ableToMove = !gameState.player.isEffectActive("stagger") &&
        !gameState.player.isEffectActive("knockback") && gameState.player.isAlive

      if (ableToMove) gameState.modify(_.creatures.at(gameState.currentPlayerId)).using { player =>
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
          .setTo(Vec2(movementVectorX, movementVectorY).normal)
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
      .pipe(handleInventoryOpenClose)
      .pipe(handleDebugButton)
      .pipe(handleCheatsButton)
      .pipe(
        gameState =>
          if (gameState.inventoryWindow.isOpen)
            gameState
              .pipe(processInventoryWindowActions)
          else
            gameState
              .pipe(handleAttackCommand)
              .pipe(handleDashCommand)
              .pipe(processLootPileMenuActions)
      )

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

    val currentArea = gameRenderer.areaRenderers(gameState.currentAreaId)
    val currentTerrain = physicsController.terrains(gameState.currentAreaId)

    currentArea.render(Array(0, 1))

    worldBatch.begin()

    gameRenderer.renderAreaGates(gameState, worldBatch)

    gameRenderer.renderLootPiles(gameState, worldBatch)

    gameRenderer.renderDeadEntities(gameState, worldBatch)

    gameRenderer.renderAliveEntities(gameState, worldBatch, debugRenderEnabled)

    worldBatch.end()

    currentArea.render(Array(2, 3))

    worldBatch.begin()

    gameRenderer.renderAbilities(gameState, worldBatch)

    worldBatch.end()

    hudBatch.begin()

    gameRenderer.renderHud(gameState, hudBatch, mousePosWindowScaled)

    val fps = Gdx.graphics.getFramesPerSecond
    Assets.defaultFont.draw(hudBatch.spriteBatch, s"$fps fps", 3, Constants.WindowHeight - 3, Color.WHITE)

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

    writer.write(gameState.asJson(JsonCodecs.encodeGameState).toString())

    writer.close()
  }

  def mousePosWindowScaled: Vec2 = {
    val v = new Vector3(Gdx.input.getX.toFloat, Gdx.input.getY.toFloat, 0f)
    hudCamera.unproject(v)
    Vec2(v.x, v.y)
  }
}
