package game

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.{SpriteBatch, TextureAtlas}
import layers.model_layer.gamestate.GameState
import layers.model_layer.gamestate.creature.{Creature, Player, Skeleton}
import layers.view_layer.updater.GameView
import screen.PlayScreen

class MyGdxGame extends Game {

  private var batch: SpriteBatch = _
  private var img: Texture = _

  var atlas: TextureAtlas = _

  var gameState: GameState = _
  var gameUpdater: GameView = _

  override def create(): Unit = {
    batch = new SpriteBatch
    img = new Texture("badlogic.jpg")

    atlas = new TextureAtlas("assets/atlas/packed_atlas.atlas")

    //TODO: add another class that hold animation parameters
    //val playerAnimation: WrAnimation = WrAnimation(atlas, "male1", 0, 0, 32, 32)

    val player: Player = Player(Creature.Params(id = "player", posX = 10, posY = 10))

    val skeleton: Skeleton = Skeleton(Creature.Params(id = "skel", posX = 4, posY = 4))

    gameState = GameState(player, nonPlayers = Map(skeleton.params.id -> skeleton))
    gameUpdater = GameView(atlas)
    //gameState = gameState.modify(_.nonPlayers).using(list => player :: list)

    val playScreen = new PlayScreen(batch, img, gameState, gameUpdater)

    setScreen(playScreen)
  }

  override def dispose(): Unit = {
    batch.dispose()
    img.dispose()
  }
}
