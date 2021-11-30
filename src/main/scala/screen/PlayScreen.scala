package screen

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.{GL20, OrthographicCamera, Texture}
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.{Box2DDebugRenderer, World}
import com.badlogic.gdx.utils.viewport.{FitViewport, Viewport}
import com.badlogic.gdx.{Gdx, Input, Screen}
import com.softwaremill.quicklens._
import layers.model_layer.gamestate.GameState
import layers.model_layer.gamestate.creature.Creature
import layers.view_layer.updater.GameView
import util.{Constants, Direction}

class PlayScreen(batch: SpriteBatch, img: Texture, var gameState: GameState, var gameUpdater: GameView) extends Screen {

  var b2DebugRenderer: Box2DDebugRenderer = new Box2DDebugRenderer()

  var world = new World(new Vector2(0, 0), true)

  val camera: OrthographicCamera = new OrthographicCamera()

  val viewport: Viewport =
    new FitViewport(
      Constants.ViewpointWorldWidth / Constants.PPM,
      Constants.ViewpointWorldHeight / Constants.PPM,
      camera
    )

  def updateCamera(): Unit = {

    val camPosition = camera.position

    camPosition.x = (math.floor(0 * 100) / 100).toFloat
    camPosition.y = (math.floor(0 * 100) / 100).toFloat

    camera.update()

  }

  override def show(): Unit = {}

  def updateCreatures(gameState: GameState): GameState = {

    val operation = (creature: Creature) => {

      val pos =
        if (gameUpdater.creatureRenderers.contains(creature.params.id))
          gameUpdater.creatureRenderers(creature.params.id).body.getWorldCenter
        else
          new Vector2(creature.params.posX, creature.params.posY)

      creature.modify(_.params.posX).setTo(pos.x).modify(_.params.posY).setTo(pos.y)
    }

    gameState
      .modify(_.player)
      .using(operation)
      .modifyAll(_.nonPlayers.each)
      .using(operation)

  }

  def update(delta: Float): Unit = {

    world.step(Math.min(Gdx.graphics.getDeltaTime, 0.15f), 6, 2) //TODO: move to area class later

    // --- update model
    val performGameStateUpdates = (identity(_: GameState)) andThen
      processPlayerMovement andThen
      updateCreatures

    gameState = performGameStateUpdates(gameState)
    // ---

    // --- update view
    gameUpdater.update(gameState, world)
    // ---

    updateCamera()
  }

  private def processPlayerMovement(gameState: GameState): GameState = {
    val directionalSpeed: Float = {
      import Input.Keys._

      val sqrt2 = 1.4142135f
      val speed = 10f

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

    Gdx.gl.glClear(
      GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT
        | (if (Gdx.graphics.getBufferFormat.coverageSampling)
             GL20.GL_COVERAGE_BUFFER_BIT_NV
           else 0)
    )

    batch.begin()
    batch.draw(img, 0, 0, 10f, 10f)

    gameUpdater.render(gameState, batch)

    batch.end()

    b2DebugRenderer.render(world, camera.combined)

  }

  override def resize(width: Int, height: Int): Unit = {
    viewport.update(width, height)
    //hudViewport.update(width, height)
  }

  override def pause(): Unit = {}

  override def resume(): Unit = {}

  override def hide(): Unit = {}

  override def dispose(): Unit = {
    batch.dispose()
    img.dispose()
  }
}
