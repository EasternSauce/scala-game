package com.easternsauce.view.area

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.{TiledMap, TmxMapLoader}
import com.easternsauce.util.Constants

case class AreaRenderer(id: String, filesDirectory: String, map: TiledMap, mapLoader: TmxMapLoader, mapScale: Float) {

  private val tiledMapRenderer: OrthogonalTiledMapRenderer =
    new OrthogonalTiledMapRenderer(map, mapScale / Constants.PPM)

  def render(layers: Array[Int]): Unit = {
    tiledMapRenderer.render(layers)
  }

  def setView(camera: OrthographicCamera): Unit = tiledMapRenderer.setView(camera)

  def dispose(): Unit = {
    tiledMapRenderer.dispose()
  }
}
