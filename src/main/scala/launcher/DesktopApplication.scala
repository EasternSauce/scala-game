package launcher

import com.badlogic.gdx.backends.lwjgl3.{Lwjgl3Application, Lwjgl3ApplicationConfiguration}
import game.MyGdxGame

object DesktopApplication {

  def main(arg: Array[String]): Unit = {

    val config = new Lwjgl3ApplicationConfiguration
    config.setWindowedMode(800, 600)
    config.setTitle("game")

    new Lwjgl3Application(new MyGdxGame, config)
  }
}
