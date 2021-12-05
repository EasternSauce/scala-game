package com.easternsauce.physics.terrain

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.maps.tiled.{TiledMap, TiledMapTileLayer}
import com.badlogic.gdx.math.{Polygon, Vector2}
import com.badlogic.gdx.physics.box2d._
import com.easternsauce.util.Constants

import scala.collection.mutable.ListBuffer

case class Terrain(map: TiledMap, mapScale: Float) {

  val world = new World(new Vector2(0, 0), true)

  private val layer = map.getLayers.get(0).asInstanceOf[TiledMapTileLayer]

  var traversable: Array[Array[Boolean]] = _
  var traversableWithMargins: Array[Array[Boolean]] = _
  var flyover: Array[Array[Boolean]] = _

  val tileWidth: Float = layer.getTileWidth * mapScale / Constants.PPM
  val tileHeight: Float = layer.getTileHeight * mapScale / Constants.PPM

  val terrainTiles: ListBuffer[TerrainTileBody] = ListBuffer()

  def init(): Unit = {

    val widthInTiles = layer.getWidth
    val heightInTiles = layer.getHeight

    traversable = Array.ofDim(heightInTiles, widthInTiles)
    traversableWithMargins = Array.ofDim(heightInTiles, widthInTiles)
    flyover = Array.ofDim(heightInTiles, widthInTiles)

    for {
      x <- 0 until widthInTiles
      y <- 0 until heightInTiles
    } traversable(y)(x) = true

    for {
      x <- 0 until widthInTiles
      y <- 0 until heightInTiles
    } traversableWithMargins(y)(x) = true

    for {
      x <- 0 until widthInTiles
      y <- 0 until heightInTiles
    } flyover(y)(x) = true

    createMapTerrain(widthInTiles, heightInTiles)
    createBorders(widthInTiles, heightInTiles)

  }

  private def createMapTerrain(widthInTiles: Int, heightInTiles: Int): Unit = {
    def tileExists(x: Int, y: Int) = x >= 0 && x < widthInTiles && y >= 0 && y < heightInTiles

    for (layerNum <- 0 to 1) { // two layers
      val layer: TiledMapTileLayer =
        map.getLayers.get(layerNum).asInstanceOf[TiledMapTileLayer]

      for {
        x <- Seq.range(0, layer.getWidth)
        y <- Seq.range(0, layer.getHeight)
      } {
        val cell: TiledMapTileLayer.Cell = layer.getCell(x, y)

        if (cell != null) {
          val isTileTraversable: Boolean =
            cell.getTile.getProperties.get("traversable").asInstanceOf[Boolean]
          val isTileFlyover: Boolean =
            cell.getTile.getProperties.get("flyover").asInstanceOf[Boolean]

          if (!isTileTraversable) {
            traversable(y)(x) = false

            traversableWithMargins(y)(x) = false

            List((0, 1), (1, 0), (-1, 0), (0, -1), (1, 1), (-1, 1), (-1, -1), (1, -1))
              .filter(pair => tileExists(x + pair._1, y + pair._2))
              .foreach(pair => traversableWithMargins(y + pair._2)(x + pair._1) = false)
          }

          if (!isTileFlyover) flyover(y)(x) = false
        }

      }

      for {
        x <- 0 until widthInTiles
        y <- 0 until heightInTiles
      } {

        if (!traversable(y)(x)) {
          val polygon = new Polygon(
            Array(
              x * tileWidth,
              y * tileHeight,
              x * tileWidth + tileWidth,
              y * tileHeight,
              x * tileWidth + tileWidth,
              y * tileHeight + tileHeight,
              x * tileWidth,
              y * tileHeight + tileHeight
            )
          )

          val tile: TerrainTileBody =
            TerrainTileBody(layerNum, x, y, tileWidth, tileHeight, flyover(y)(x), polygon)

          tile.init(world)

          terrainTiles += tile
        }
      }

    }
  }

  private def createBorders(widthInTiles: Int, heightInTiles: Int): Unit = {

    for { x <- Seq.range(0, widthInTiles) } {
      createBorderTile(x, -1)
      createBorderTile(x, heightInTiles)
    }

    for { y <- Seq.range(0, heightInTiles) } {
      createBorderTile(-1, y)
      createBorderTile(widthInTiles, y)
    }
  }

  private def createBorderTile(x: Int, y: Int): Unit = {
    val bodyDef = new BodyDef()
    bodyDef.`type` = BodyDef.BodyType.StaticBody
    val tileCenter = getTileCenter(x, y)
    bodyDef.position.set(tileCenter.x, tileCenter.y)

    val body: Body = world.createBody(bodyDef)

    body.setUserData(this)

    val shape: PolygonShape = new PolygonShape()

    shape.setAsBox(tileWidth / 2, tileHeight / 2)

    val fixtureDef: FixtureDef = new FixtureDef

    fixtureDef.shape = shape

    body.createFixture(fixtureDef)
  }

  def getTileCenter(x: Int, y: Int): Vector2 = {
    new Vector2(x * tileWidth + tileWidth / 2, y * tileHeight + tileHeight / 2)
  }

  def getClosestTile(x: Float, y: Float): Vector2 = {
    new Vector2(x / tileWidth, y / tileHeight)
  }

  def step(): Unit = world.step(Math.min(Gdx.graphics.getDeltaTime, 0.15f), 6, 2)

  def dispose(): Unit = world.dispose()
}
