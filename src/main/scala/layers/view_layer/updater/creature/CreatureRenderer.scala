package layers.view_layer.updater.creature

import com.badlogic.gdx.graphics.g2d._
import com.badlogic.gdx.physics.box2d.{Body, BodyDef, CircleShape, FixtureDef, World}
import layers.model_layer.gamestate.GameState
import util.Direction

case class CreatureRenderer(id: String, atlas: TextureAtlas) {

  val sprite: Sprite = new Sprite()

  var facingTextures: Array[TextureRegion] = new Array[TextureRegion](4)

  var runningAnimations: Array[Animation[TextureRegion]] = new Array[Animation[TextureRegion]](4)

  var textureRegion: TextureRegion = _

  var body: Body = _

  // var initialized = false TODO: is it needed?


  def initBody(world: World): Body = {
    val bodyDef = new BodyDef()
    bodyDef.position.set(5, 5)

    bodyDef.`type` = BodyDef.BodyType.KinematicBody
    val b2Body = world.createBody(bodyDef)
    b2Body.setUserData(this)

    val fixtureDef: FixtureDef = new FixtureDef()
    val shape: CircleShape = new CircleShape()
    shape.setRadius(2f)

    fixtureDef.shape = shape
    fixtureDef.isSensor = false
    b2Body.createFixture(fixtureDef)


    b2Body
  }

  def init(gameState: GameState, world: World): Unit = {
    val creature = gameState.creatures(id)

    val spriteData = creature.params.spriteTextureData
    val animData = creature.params.animationData

    textureRegion = atlas.findRegion(spriteData.spriteType)

    for (i <- 0 until 4) {
      facingTextures(i) = new TextureRegion(
        textureRegion,
        animData.neutralStanceFrame * spriteData.textureWidth,
        i * spriteData.textureHeight,
        spriteData.textureWidth,
        spriteData.textureHeight
      )

    }

    for (i <- 0 until 4) {
      val frames = for {j <- (0 until animData.frameCount).toArray} yield {
        new TextureRegion(
          textureRegion,
          j * spriteData.textureWidth,
          i * spriteData.textureHeight,
          spriteData.textureHeight,
          spriteData.textureHeight
        )
      }
      runningAnimations(i) = new Animation[TextureRegion](animData.frameDuration, frames: _*)

    }

    body = initBody(world)

  }

  def runningAnimation(gameState: GameState, currentDirection: Direction.Value): TextureRegion = {
    val creature = gameState.creatures(id)

    runningAnimations(creature.params.spriteTextureData.dirMap(currentDirection)).getKeyFrame(gameState.player.params.animationTimer.time, true)
  }

  def facingTexture(gameState: GameState, currentDirection: Direction.Value): TextureRegion = {
    val creature = gameState.creatures(id)

    facingTextures(creature.params.spriteTextureData.dirMap(currentDirection))
  }

  def update(gameState: GameState, world: World): Unit = {
    val creature = gameState.creatures(id)
    val spriteInfo = creature.params.spriteTextureData

    val texture = if (!creature.params.isMoving) facingTexture(gameState, creature.params.facingDirection)
    else runningAnimation(gameState, creature.params.facingDirection)
    sprite.setRegion(texture)
    sprite.setBounds(creature.params.posX, creature.params.posY, spriteInfo.boundsWidth, spriteInfo.boundsHeight)

    body.setLinearVelocity(0.5f, 0.5f)


  }

  def render(batch: SpriteBatch): Unit = {
    sprite.draw(batch)
  }
}
