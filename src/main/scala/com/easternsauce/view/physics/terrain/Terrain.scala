package com.easternsauce.view.physics.terrain

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.maps.tiled.{TiledMap, TiledMapTileLayer}
import com.badlogic.gdx.math.{Intersector, Polygon, Vector2}
import com.badlogic.gdx.physics.box2d._
import com.easternsauce.util.{Constants, Vector2Wrapper}
import com.easternsauce.view.pathfinding.{Astar, PathingNode}

import scala.collection.mutable.ListBuffer

case class Terrain(map: TiledMap, mapScale: Float) {
  val world = new World(new Vector2(0, 0), true)

  private val layer = map.getLayers.get(0).asInstanceOf[TiledMapTileLayer]

  var traversable: Map[Vector2Wrapper, Boolean] = Map()
  var traversableWithMargins: Map[Vector2Wrapper, Boolean] = Map()
//  var traversableWithMargins: Array[Array[Boolean]] = _
  var flyover: Map[Vector2Wrapper, Boolean] = Map()

  var clearances: Map[Vector2Wrapper, Int] = Map()

  val tileWidth: Float = layer.getTileWidth * mapScale / Constants.PPM
  val tileHeight: Float = layer.getTileHeight * mapScale / Constants.PPM

  val terrainTiles: ListBuffer[TerrainTileBody] = ListBuffer()
  val terrainBorders: ListBuffer[TerrainTileBody] = ListBuffer()

  def widthInTiles: Int = layer.getWidth
  def heightInTiles: Int = layer.getHeight

  var pathingGraph: Map[Vector2Wrapper, PathingNode] = _

  def init(): Unit = {
    //traversable = Array.ofDim(heightInTiles, widthInTiles)
//    traversableWithMargins = Array.ofDim(heightInTiles, widthInTiles)
//    flyover = Array.ofDim(heightInTiles, widthInTiles)

    for {
      x <- 0 until widthInTiles
      y <- 0 until heightInTiles
    } traversable = traversable + (Vector2Wrapper(x, y) -> true)

//    traversable.
//    while ()

    for {
      x <- 0 until widthInTiles
      y <- 0 until heightInTiles
    } traversableWithMargins = traversableWithMargins + (Vector2Wrapper(x, y) -> true)

    for {
      x <- 0 until widthInTiles
      y <- 0 until heightInTiles
    } flyover = flyover + (Vector2Wrapper(x, y) -> true)

    createMapTerrain(widthInTiles, heightInTiles)
    createBorders(widthInTiles, heightInTiles)

    pathingGraph = Astar.generatePathingGraph(this)
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
            traversable = traversable + (Vector2Wrapper(x, y) -> false)

            traversableWithMargins = traversableWithMargins + (Vector2Wrapper(x, y) -> false)

            List((0, 1), (1, 0), (-1, 0), (0, -1), (1, 1), (-1, 1), (-1, -1), (1, -1))
              .filter(pair => tileExists(x + pair._1, y + pair._2))
              .foreach(
                pair =>
                  traversableWithMargins = traversableWithMargins + (Vector2Wrapper(y + pair._1, x + pair._2) -> false)
              )
          }

          if (!isTileFlyover) flyover = flyover + (Vector2Wrapper(x, y) -> false)
        }

      }

      for {
        x <- 0 until widthInTiles
        y <- 0 until heightInTiles
      } {

        if (!traversable(Vector2Wrapper(x, y))) {
          val tile: TerrainTileBody =
            TerrainTileBody(x, y, tileWidth, tileHeight, layerNum, flyover(Vector2Wrapper(x, y)))

          tile.init(world)

          terrainTiles += tile
        }
      }

    }




    def tryAddClearance(pos: Vector2Wrapper, level: Int): Unit = {
      if (
        !clearances.contains(
          pos
        ) && pos.x >= 0 && pos.y >= 0 && pos.x < widthInTiles && pos.y < heightInTiles && traversable(pos)
      )
        clearances = clearances + (pos -> level)
    }
    for {
      x <- 0 until widthInTiles
      y <- 0 until heightInTiles
    } {
      if (!traversable(Vector2Wrapper(x, y))) {
        tryAddClearance(Vector2Wrapper(x - 1, y - 1), 1)
        tryAddClearance(Vector2Wrapper(x, y - 1), 1)
        tryAddClearance(Vector2Wrapper(x + 1, y - 1), 1)
        tryAddClearance(Vector2Wrapper(x - 1, y + 1), 1)
        tryAddClearance(Vector2Wrapper(x, y + 1), 1)
        tryAddClearance(Vector2Wrapper(x + 1, y + 1), 1)
        tryAddClearance(Vector2Wrapper(x - 1, y), 1)
        tryAddClearance(Vector2Wrapper(x + 1, y), 1)
      }
    }

    var currentLevel = 2
    while (traversable.values.count(_ == true) != clearances.size) {

      val lowerLevelClearances =
        clearances.filter { case (_, clearanceLevel) => clearanceLevel == currentLevel - 1 }.map {
          case (pos, _) => pos
        }

      lowerLevelClearances.foreach {
        case Vector2Wrapper(x, y) =>
          tryAddClearance(Vector2Wrapper(x - 1, y - 1), currentLevel)
          tryAddClearance(Vector2Wrapper(x, y - 1), currentLevel)
          tryAddClearance(Vector2Wrapper(x + 1, y - 1), currentLevel)
          tryAddClearance(Vector2Wrapper(x - 1, y + 1), currentLevel)
          tryAddClearance(Vector2Wrapper(x, y + 1), currentLevel)
          tryAddClearance(Vector2Wrapper(x + 1, y + 1), currentLevel)
          tryAddClearance(Vector2Wrapper(x - 1, y), currentLevel)
          tryAddClearance(Vector2Wrapper(x + 1, y), currentLevel)
      }

      currentLevel = currentLevel + 1
    }
  }

  private def createBorders(widthInTiles: Int, heightInTiles: Int): Unit = {

    for { x <- Seq.range(0, widthInTiles) } {
      terrainBorders += TerrainTileBody(x, -1, tileWidth, tileHeight)
      terrainBorders += TerrainTileBody(x, heightInTiles, tileWidth, tileHeight)
    }

    for { y <- Seq.range(0, heightInTiles) } {
      terrainBorders += TerrainTileBody(-1, y, tileWidth, tileHeight)
      terrainBorders += TerrainTileBody(widthInTiles, y, tileWidth, tileHeight)
    }

    terrainBorders.foreach(_.init(world))
  }

  def getTileCenter(pos: Vector2Wrapper): Vector2Wrapper = {
    Vector2Wrapper(pos.x * tileWidth + tileWidth / 2, pos.y * tileHeight + tileHeight / 2)
  }

  def getClosestTile(pos: Vector2Wrapper): Vector2Wrapper = {
    Vector2Wrapper((pos.x / tileWidth).toInt, (pos.y / tileHeight).toInt)
  }

  def isLineOfSight(fromPos: Vector2Wrapper, toPos: Vector2Wrapper): Boolean = {
    val lineWidth = 0.3f

    val lineOfSightRect =
      new Polygon(
        Array(
          fromPos.x,
          fromPos.y,
          fromPos.x + lineWidth,
          fromPos.y + lineWidth,
          toPos.x + lineWidth,
          toPos.y + lineWidth,
          toPos.x,
          toPos.y
        )
      )

    terrainTiles
      .map(tile => tile.polygon)
      .forall(!Intersector.overlapConvexPolygons(_, lineOfSightRect))
  }

  def step(): Unit = world.step(Math.min(Gdx.graphics.getDeltaTime, 0.15f), 6, 2)

  def dispose(): Unit = world.dispose()
}
