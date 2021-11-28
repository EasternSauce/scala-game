package launcher

import com.badlogic.gdx.backends.lwjgl3.{Lwjgl3Application, Lwjgl3ApplicationConfiguration}
import game.MyGdxGame
import util.Constants

object DesktopApplication {

  def main(arg: Array[String]): Unit = {

    val config = new Lwjgl3ApplicationConfiguration
    config.setWindowedMode(Constants.WindowWidth, Constants.WindowHeight)
    config.setTitle("game")

    new Lwjgl3Application(new MyGdxGame, config)
  }
}
