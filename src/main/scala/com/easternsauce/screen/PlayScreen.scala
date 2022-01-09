package com.easternsauce.screen

import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.graphics.{GL20, OrthographicCamera}
import com.badlogic.gdx.math.{Vector2, Vector3}
import com.badlogic.gdx.physics.box2d._
import com.badlogic.gdx.utils.viewport.{FitViewport, Viewport}
import com.badlogic.gdx.{Gdx, Input, Screen}
import com.easternsauce.box2d_physics.PhysicsController
import com.easternsauce.event.{AreaChangeEvent, CollisionEvent}
import com.easternsauce.model.{GameState, InventoryState}
import com.easternsauce.model.area.Area
import com.easternsauce.model.creature.ability.AbilityState.AbilityState
import com.easternsauce.model.creature.ability.{Ability, AbilityHitbox, AbilityParams, AbilityState}
import com.easternsauce.model.creature.{Creature, CreatureParams}
import com.easternsauce.model.event.UpdateEvent
import com.easternsauce.model.item.{Item, ItemParameterValue, ItemTemplate}
import com.easternsauce.model.util.SimpleTimer
import com.easternsauce.util.Direction.Direction
import com.easternsauce.util.{Constants, Direction, RendererBatch, Vector2Wrapper}
import com.easternsauce.view.GameView
import com.softwaremill.quicklens._
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps

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

  val debugRenderEnabled = false

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

  implicit val decodeGameStateSave: Decoder[GameState] = deriveDecoder
  implicit val encodeGameStateSave: Encoder[GameState] = deriveEncoder
  implicit val decodeCreature: Decoder[Creature] = deriveDecoder
  implicit val encodeCreature: Encoder[Creature] = deriveEncoder
  implicit val decodeArea: Decoder[Area] = deriveDecoder
  implicit val encodeArea: Encoder[Area] = deriveEncoder
  implicit val decodeUpdateEvent: Decoder[UpdateEvent] = deriveDecoder
  implicit val encodeUpdateEvent: Encoder[UpdateEvent] = deriveEncoder
  implicit val decodeInventoryState: Decoder[InventoryState] = deriveDecoder
  implicit val encodeInventoryState: Encoder[InventoryState] = deriveEncoder
  implicit val decodeCreatureParams: Decoder[CreatureParams] = deriveDecoder
  implicit val encodeCreatureParams: Encoder[CreatureParams] = deriveEncoder
  implicit val decodeSimpleTimer: Decoder[SimpleTimer] = deriveDecoder
  implicit val encodeSimpleTimer: Encoder[SimpleTimer] = deriveEncoder
  implicit val decodeItem: Decoder[Item] = deriveDecoder
  implicit val encodeItem: Encoder[Item] = deriveEncoder
  implicit val decodeItemTemplate: Decoder[ItemTemplate] = deriveDecoder
  implicit val encodeItemTemplate: Encoder[ItemTemplate] = deriveEncoder
  implicit val decodeItemParameterValue: Decoder[ItemParameterValue] = deriveDecoder
  implicit val encodeItemParameterValue: Encoder[ItemParameterValue] = deriveEncoder
  implicit val decodeAbilityParams: Decoder[AbilityParams] = deriveDecoder
  implicit val encodeAbilityParams: Encoder[AbilityParams] = deriveEncoder
  implicit val decodeAbilityHitbox: Decoder[AbilityHitbox] = deriveDecoder
  implicit val encodeAbilityHitbox: Encoder[AbilityHitbox] = deriveEncoder
  implicit val decodeAbilityState: Decoder[AbilityState] = Decoder.decodeEnumeration(AbilityState)
  implicit val encodeAbilityState: Encoder[AbilityState] = Encoder.encodeEnumeration(AbilityState)
  implicit val decodeVector2Wrapper: Decoder[Vector2Wrapper] = deriveDecoder
  implicit val encodeVector2Wrapper: Encoder[Vector2Wrapper] = deriveEncoder
  implicit val decodeDirection: Decoder[Direction] = Decoder.decodeEnumeration(Direction)
  implicit val encodeDirection: Encoder[Direction] = Encoder.encodeEnumeration(Direction)

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
      .pipe(processPlayerInput)
      .pipe(processInventoryActions)
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

  private def processPlayerInput(gameState: GameState): GameState = {
    // TODO: temporarily simulate creature changing areas on SPACE
    if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
      if (gameState.currentAreaId == "area1") {
        areaChangeQueue.prepend(AreaChangeEvent("player", "area1", "area2"))
      } else {
        areaChangeQueue.prepend(AreaChangeEvent("player", "area2", "area1"))
      }
    }

    val handleInventoryOpen: GameState => GameState = gameState => {
      val inventoryOpen = gameState.inventoryState.inventoryOpen
      if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
        if (!inventoryOpen) {
          gameState.modify(_.inventoryState.inventoryOpen).setTo(true)
        } else { gameState.modify(_.inventoryState.inventoryOpen).setTo(false) }

      } else gameState
    }

    val leftClickInput: GameState => GameState =
      if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
        val mouseX = Gdx.input.getX
        val mouseY = Gdx.input.getY

        val centerX = Gdx.graphics.getWidth / 2f
        val centerY = Gdx.graphics.getHeight / 2f

        val facingVector = new Vector2(mouseX - centerX, (Gdx.graphics.getHeight - mouseY) - centerY).nor()

        val modification: GameState => GameState =
          _.modifyGameStateCreature("player")(_.modify(_.params.dirVector).setTo(Vector2Wrapper(facingVector.x, facingVector.y)))
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
      .pipe(handleInventoryOpen)

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

    println("saving game")
  }

  def mousePosWindowScaled: Vector2 = {
    val v = new Vector3(Gdx.input.getX.toFloat, Gdx.input.getY.toFloat, 0f)
    hudCamera.unproject(v)
    new Vector2(v.x, v.y)
  }
}
