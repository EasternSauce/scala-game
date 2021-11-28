package game

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.{SpriteBatch, TextureAtlas}
import data.{AnimationData, SpriteTextureData}
import layers.model_layer.gamestate.GameState
import layers.model_layer.gamestate.creature.{Creature, Player}
import layers.view_layer.updater.GameUpdater
import screen.PlayScreen
import util.Direction

class MyGdxGame extends Game {

  private var batch: SpriteBatch = _
  private var img: Texture = _

  var atlas: TextureAtlas = _

  var gameState: GameState = _
  var gameUpdater: GameUpdater = _

  override def create(): Unit = {
    batch = new SpriteBatch
    img = new Texture("badlogic.jpg")

    atlas = new TextureAtlas("assets/atlas/packed_atlas.atlas")

    //TODO: add another class that hold animation parameters
    //val playerAnimation: WrAnimation = WrAnimation(atlas, "male1", 0, 0, 32, 32)

    val male1SpriteData = SpriteTextureData(
      spriteType = "male1",
      boundsWidth = 64,
      boundsHeight = 64,
      textureWidth = 32,
      textureHeight = 32,
      dirMap = Map(Direction.Up -> 3, Direction.Down -> 0, Direction.Left -> 1, Direction.Right -> 2)
    )
    val male1AnimationData = AnimationData(frameDuration = 0.1f, frameCount = 3, neutralStanceFrame = 1)

    val player: Player = Player(
      Creature.Params(
        id = "player",
        posX = 233,
        posY = 235,
        spriteTextureData = male1SpriteData,
        animationData = male1AnimationData
      )
    )

    gameState = GameState(player)
    gameUpdater = GameUpdater(atlas)
    //gameState = gameState.modify(_.nonPlayers).using(list => player :: list)

    val playScreen = new PlayScreen(batch, img, gameState, gameUpdater)

    setScreen(playScreen)
  }

  override def dispose(): Unit = {
    batch.dispose()
    img.dispose()
  }
}
