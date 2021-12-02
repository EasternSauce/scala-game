package com.easternsauce.view.area

import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.{TiledMap, TiledMapTileLayer, TmxMapLoader}
import com.badlogic.gdx.math.{Polygon, Vector2}
import com.badlogic.gdx.physics.box2d._
import com.easternsauce.util.Constants
import com.easternsauce.view.area

import scala.collection.mutable.ListBuffer

case class AreaRenderer(id: String, filesDirectory: String, mapLoader: TmxMapLoader) {
  var world = new World(new Vector2(0, 0), true)
  val mapScale = 4.0f
  val map: TiledMap = mapLoader.load(filesDirectory + "/tile_map.tmx")
  val tiledMapRenderer: OrthogonalTiledMapRenderer =
    new OrthogonalTiledMapRenderer(map, mapScale / Constants.PPM)

  var widthInTiles: Int = _
  var heightInTiles: Int = _
  var tileWidth: Float = _
  var tileHeight: Float = _

  val terrainTiles: ListBuffer[AreaTileBody] = ListBuffer()

  var traversable: Array[Array[Boolean]] = _
  var traversableWithMargins: Array[Array[Boolean]] = _
  var flyover: Array[Array[Boolean]] = _

  def initPhysicalTerrain(): Unit = {
    val layer = map.getLayers.get(0).asInstanceOf[TiledMapTileLayer]

    widthInTiles = layer.getWidth
    heightInTiles = layer.getHeight

    tileWidth = layer.getTileWidth * mapScale / Constants.PPM
    tileHeight = layer.getTileHeight * mapScale / Constants.PPM

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

    createMapTerrain()
    createBorders()

  }

  private def createMapTerrain(): Unit = {
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

          val tile: AreaTileBody = area.AreaTileBody(layerNum, x, y, tileWidth, tileHeight, flyover(y)(x), polygon)

          tile.init(world)

          terrainTiles += tile
        }
      }

    }
  }

  private def createBorders(): Unit = {

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

  def dispose(): Unit = {
    tiledMapRenderer.dispose()
    world.dispose()
  }
}
