package game

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.{SpriteBatch, TextureAtlas}
import model.GameState
import model.creature.{Creature, Player, Skeleton}
import screen.PlayScreen
import view.GameView

class MyGdxGame extends Game {

  private var batch: SpriteBatch = _

  var atlas: TextureAtlas = _

  var gameState: GameState = _
  var gameUpdater: GameView = _

  override def create(): Unit = {
    batch = new SpriteBatch

    atlas = new TextureAtlas("assets/atlas/packed_atlas.atlas")

    val player: Player = Player(Creature.Params(id = "player", posX = 10, posY = 10))

    val skeleton: Skeleton = Skeleton(Creature.Params(id = "skel", posX = 4, posY = 4))

    gameState = GameState(player, nonPlayers = Map(skeleton.params.id -> skeleton))
    gameUpdater = view.GameView(atlas)

    val playScreen = new PlayScreen(batch, gameState, gameUpdater)

    setScreen(playScreen)
  }

  override def dispose(): Unit = {
    batch.dispose()
  }
}
