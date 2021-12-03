package com.easternsauce.view.area

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.tiled.{TiledMap, TmxMapLoader}
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World

case class Area(id: String, filesDirectory: String, mapLoader: TmxMapLoader) {

  val world = new World(new Vector2(0, 0), true)

  private val mapScale = 4.0f

  private val map: TiledMap = mapLoader.load(filesDirectory + "/tile_map.tmx")

  private val renderer: AreaRenderer = AreaRenderer(id, filesDirectory, map, mapLoader, mapScale)
  private val terrain: Terrain = Terrain(world, map, mapScale)

  terrain.init()

  def step(): Unit = world.step(Math.min(Gdx.graphics.getDeltaTime, 0.15f), 6, 2)

  def setView(camera: OrthographicCamera): Unit = renderer.setView(camera)

  def render(layers: Array[Int]): Unit = renderer.render(layers)

  def dispose(): Unit = {
    renderer.dispose()
    world.dispose()
  }
}
