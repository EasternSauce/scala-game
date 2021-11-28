package screen

import com.badlogic.gdx.graphics.{OrthographicCamera, Texture}
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.{Body, BodyDef, Box2DDebugRenderer, CircleShape, FixtureDef, PolygonShape, World}
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.{FitViewport, Viewport}
import com.badlogic.gdx.{Gdx, Input, Screen}
import com.softwaremill.quicklens.ModifyPimp
import layers.model_layer.gamestate.GameState
import layers.model_layer.gamestate.creature.Player
import layers.view_layer.updater.GameUpdater
import util.Direction
import util.Constants


class PlayScreen(batch: SpriteBatch, img: Texture, var gameState: GameState, var gameUpdater: GameUpdater)
    extends Screen {

  var b2DebugRenderer: Box2DDebugRenderer = new Box2DDebugRenderer()

  var world = new World(new Vector2(0, 0), true)

  val camera: OrthographicCamera = new OrthographicCamera()

  val viewport: Viewport =
    new FitViewport(
      Constants.ViewpointWorldWidth / Constants.PPM,
      Constants.ViewpointWorldHeight / Constants.PPM,
      camera
    )

  initBody(world)

  def updateCamera(): Unit = {

    val camPosition = camera.position

    camPosition.x = (math.floor(0 * 100) / 100).toFloat
    camPosition.y = (math.floor(0 * 100) / 100).toFloat

    camera.update()

  }

  def initBody(world: World): Option[Body] = {
    val bodyDef = new BodyDef()
    bodyDef.position.set(0, 0)

    bodyDef.`type` = BodyDef.BodyType.KinematicBody
    val b2Body = world.createBody(bodyDef)
    b2Body.setUserData(this)

    val fixtureDef: FixtureDef = new FixtureDef()
    val shape: CircleShape = new CircleShape()
    shape.setRadius(2f)

    fixtureDef.shape = shape
    fixtureDef.isSensor = false
    b2Body.createFixture(fixtureDef)

    Some(b2Body)
  }

  override def show(): Unit = {}

  def update(delta: Float): Unit = {
    updatePlayerPosition()

    updateGameState(
      gameState.modify(_.player)
      .using(_.update(delta))
    )

    gameUpdater.update(gameState)

    updateCamera()
  }

  private def updatePlayerPosition(): Unit = {
    val oldPosX = gameState.player.params.posX
    val oldPosY = gameState.player.params.posY

    val directionalSpeed: Float = {
      import Input.Keys._

      val sqrt2 = 1.4142135f
      val speed = 2.0f

      val directionalSpeed = List(W, S, A, D).map(Gdx.input.isKeyPressed(_)) match {
        case List(true, _, true, _) => speed / sqrt2
        case List(true, _, _, true) => speed / sqrt2
        case List(_, true, true, _) => speed / sqrt2
        case List(_, true, _, true) => speed / sqrt2
        case _                      => speed
      }
      directionalSpeed
    }

    val (newPosX: Float, newPosY: Float) = {
      import Input.Keys._

      val x: Float = List(A, D).map(Gdx.input.isKeyPressed(_)) match {
        case List(true, false) => oldPosX - directionalSpeed
        case List(false, true) => oldPosX + directionalSpeed
        case _                 => oldPosX
      }

      val y: Float = List(S, W).map(Gdx.input.isKeyPressed(_)) match {
        case List(true, false) => oldPosY - directionalSpeed
        case List(false, true) => oldPosY + directionalSpeed
        case _                 => oldPosY
      }

      (x, y)
    }

    val facingDirection = {
      import Input.Keys._
      List(W, S, A, D).map(Gdx.input.isKeyPressed(_)) match {
        case List(true, _, _, _) => Direction.Up
        case List(_, true, _, _) => Direction.Down
        case List(_, _, true, _) => Direction.Left
        case List(_, _, _, true) => Direction.Right
        case _                   => gameState.player.params.facingDirection
      }

    }

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

    val wasMoving = gameState.player.params.isMoving
    val startMovingAction = (wasMoving, isMoving) match {
      case (false, true) => player: Player => {
        player.modify(_.params.animationTimer).using(_.restart())
      }
      case _ => player: Player => player
    }

    updateGameState(
      gameState
        .modify(_.player.params.posX)
        .setTo(newPosX)
        .modify(_.player.params.posY)
        .setTo(newPosY)
        .modify(_.player.params.facingDirection)
        .setTo(facingDirection)
        .modify(_.player.params.isMoving)
        .setTo(isMoving)
        .modify(_.player)
        .using(startMovingAction)
    )
  }

  def updateGameState(gameState: GameState): Unit = {
    this.gameState = gameState
  }

  override def render(delta: Float): Unit = {
    update(delta)

    ScreenUtils.clear(1, 1, 1, 1)
    batch.begin()
    batch.draw(img, 10, 10)

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
