package com.easternsauce.view.renderer

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.easternsauce.util.Constants

case class AreaRenderer(id: String, map: TiledMap, mapScale: Float) {

  private var tiledMapRenderer: OrthogonalTiledMapRenderer = _

  def init(): Unit = {
    tiledMapRenderer = new OrthogonalTiledMapRenderer(map, mapScale / Constants.PPM)
  }

  def render(layers: Array[Int]): Unit = {
    tiledMapRenderer.render(layers)
  }

  def setView(camera: OrthographicCamera): Unit = tiledMapRenderer.setView(camera)

  def dispose(): Unit = {
    tiledMapRenderer.dispose()
  }
}
