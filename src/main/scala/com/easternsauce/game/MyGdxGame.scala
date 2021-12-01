package com.easternsauce.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.{SpriteBatch, TextureAtlas}
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.easternsauce.model.GameState
import com.easternsauce.model.creature.{Creature, Player, Skeleton}
import com.easternsauce.screen.PlayScreen
import com.easternsauce.view.GameView

class MyGdxGame extends Game {

  private var batch: SpriteBatch = _

  var atlas: TextureAtlas = _

  var gameState: GameState = _
  var gameView: GameView = _

  var mapLoader: TmxMapLoader = _

  override def create(): Unit = {
    batch = new SpriteBatch

    atlas = new TextureAtlas("assets/atlas/packed_atlas.atlas")

    val player: Player = Player(Creature.Params(id = "player", posX = 10, posY = 10))

    val skeleton: Skeleton = Skeleton(Creature.Params(id = "skel", posX = 4, posY = 4))

    gameState = GameState(player, nonPlayers = Map(skeleton.params.id -> skeleton), "area1")
    gameView = GameView(atlas)

    mapLoader = new TmxMapLoader()

    gameView.init(mapLoader)

    val playScreen = new PlayScreen(batch, gameState, gameView)

    setScreen(playScreen)
  }

  override def dispose(): Unit = {
    batch.dispose()
  }
}
