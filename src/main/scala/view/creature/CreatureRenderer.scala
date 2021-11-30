package view.creature

import com.badlogic.gdx.graphics.g2d._
import com.badlogic.gdx.physics.box2d._
import model.GameState
import util.Direction
import view.GameView

case class CreatureRenderer(gameView: GameView, id: String, atlas: TextureAtlas) {

  val sprite: Sprite = new Sprite()

  var facingTextures: Array[TextureRegion] = new Array[TextureRegion](4)

  var runningAnimations: Array[Animation[TextureRegion]] = new Array[Animation[TextureRegion]](4)

  var textureRegion: TextureRegion = _

  var body: Body = _

  def initBody(world: World, gameState: GameState): Body = {
    val creature = gameState.creatures(id)

    val bodyDef = new BodyDef()
    bodyDef.position.set(creature.params.posX, creature.params.posY)

    bodyDef.`type` = BodyDef.BodyType.DynamicBody
    val b2Body = world.createBody(bodyDef)
    b2Body.setUserData(this)
    b2Body.setSleepingAllowed(false)

    val fixtureDef: FixtureDef = new FixtureDef()
    val shape: CircleShape = new CircleShape()
    shape.setRadius(creature.width / 2)

    fixtureDef.shape = shape
    fixtureDef.isSensor = false
    b2Body.createFixture(fixtureDef)
    b2Body.setLinearDamping(10f)

    b2Body
  }

  def init(gameState: GameState, world: World): Unit = {
    val creature = gameState.creatures(id)

    textureRegion = atlas.findRegion(creature.spriteType)

    for (i <- 0 until 4) {
      facingTextures(i) = new TextureRegion(
        textureRegion,
        creature.neutralStanceFrame * creature.textureWidth,
        i * creature.textureHeight,
        creature.textureWidth,
        creature.textureHeight
      )

    }

    for (i <- 0 until 4) {
      val frames = for { j <- (0 until creature.frameCount).toArray } yield {
        new TextureRegion(
          textureRegion,
          j * creature.textureWidth,
          i * creature.textureHeight,
          creature.textureHeight,
          creature.textureHeight
        )
      }
      runningAnimations(i) = new Animation[TextureRegion](creature.frameDuration, frames: _*)

    }

    body = initBody(world, gameState)

  }

  def runningAnimation(gameState: GameState, currentDirection: Direction.Value): TextureRegion = {
    val creature = gameState.creatures(id)

    runningAnimations(creature.dirMap(currentDirection))
      .getKeyFrame(gameState.player.params.animationTimer.time, true)
  }

  def facingTexture(gameState: GameState, currentDirection: Direction.Value): TextureRegion = {
    val creature = gameState.creatures(id)

    facingTextures(creature.dirMap(currentDirection))
  }

  def update(gameState: GameState, world: World): Unit = {
    val creature = gameState.creatures(id)
    val texture =
      if (!creature.params.isMoving) facingTexture(gameState, creature.params.facingDirection)
      else runningAnimation(gameState, creature.params.facingDirection)
    sprite.setRegion(texture)
    sprite.setCenter(creature.params.posX, creature.params.posY)
    sprite.setSize(creature.width, creature.height)

  }

  def render(batch: SpriteBatch): Unit = {
    sprite.draw(batch)
  }
}
