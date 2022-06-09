package com.easternsauce.pathfinding

import com.easternsauce.view.physics.terrain.Terrain

import scala.collection.immutable.TreeMap
import scala.util.chaining.scalaUtilChainingOps

object Astar {
  def generateGraph(terrain: Terrain): TreeMap[(Int, Int), AstarNode] = {
    val elems =
      (for (x <- 0 until terrain.widthInTiles; y <- 0 until terrain.heightInTiles)
        yield (x, y) -> AstarNode(x, y))

    val astarNodes: TreeMap[(Int, Int), AstarNode] = TreeMap[(Int, Int), AstarNode](elems: _*)

    def tryAddingEdge(
      astarNodes: TreeMap[(Int, Int), AstarNode],
      terrain: Terrain,
      fromX: Int,
      fromY: Int,
      toX: Int,
      toY: Int,
      weight: Int
    ): TreeMap[(Int, Int), AstarNode] = {
      if (0 <= toY && toY < terrain.heightInTiles && 0 <= toX && toX < terrain.widthInTiles) {
        if (terrain.traversable(fromY)(fromX) && terrain.traversable(toY)(toX)) {
          val targetNode = astarNodes((toX, toY))
          astarNodes.updated((fromX, fromY), astarNodes((fromX, fromY)).addEdge(weight, targetNode))
        } else astarNodes
      } else astarNodes
    }

    val straightWeight = 10
    val diagonalWeight = 14

    (for {
      x <- 0 until terrain.widthInTiles
      y <- 0 until terrain.heightInTiles
    } yield (x, y)).foldLeft(astarNodes) {
      case (astarNodes, (x, y)) => {
        astarNodes
          .pipe(tryAddingEdge(_, terrain, x, y, x - 1, y, straightWeight))
          .pipe(tryAddingEdge(_, terrain, x, y, x + 1, y, straightWeight))
          .pipe(tryAddingEdge(_, terrain, x, y, x, y - 1, straightWeight))
          .pipe(tryAddingEdge(_, terrain, x, y, x, y + 1, straightWeight))
          .pipe(
            astarNodes =>
              if (
                x - 1 >= 0 && y - 1 >= 0
                && terrain.traversable(y)(x - 1) && terrain.traversable(y - 1)(x)
              ) tryAddingEdge(astarNodes, terrain, x, y, x - 1, y - 1, diagonalWeight)
              else astarNodes
          )
          .pipe(
            astarNodes =>
              if (
                x + 1 < terrain.widthInTiles && y - 1 >= 0
                && terrain.traversable(y)(x + 1) && terrain.traversable(y - 1)(x)
              ) tryAddingEdge(astarNodes, terrain, x, y, x + 1, y - 1, diagonalWeight)
              else astarNodes
          )
          .pipe(
            astarNodes =>
              if (
                x - 1 >= 0 && y + 1 < terrain.heightInTiles
                && terrain.traversable(y)(x - 1) && terrain.traversable(y + 1)(x)
              ) tryAddingEdge(astarNodes, terrain, x, y, x - 1, y + 1, diagonalWeight)
              else astarNodes
          )
          .pipe(
            astarNodes =>
              if (
                x + 1 < terrain.widthInTiles && y + 1 < terrain.heightInTiles
                && terrain.traversable(y)(x + 1) && terrain.traversable(y + 1)(x)
              ) tryAddingEdge(astarNodes, terrain, x, y, x + 1, y + 1, diagonalWeight)
              else astarNodes
          )
      }
    }
  }

}

case class AstarNode(
                      x: Int,
                      y: Int,
                      parent: Option[AstarNode] = None,
                      originatingEdges: List[AstarEdge] = List(),
                      f: Double = Double.MaxValue,
                      g: Double = Double.MaxValue
) {

  def addEdge(weight: Int, node: AstarNode): AstarNode = {
    val newEdge = AstarEdge(weight, node)
    AstarNode(x, y, parent, newEdge :: originatingEdges, f, g)
  }
  def calculateHeuristic(target: AstarNode): Double = {
    (Math.abs(target.x - x) + Math.abs(target.y - y)) * 10
  }

  override def toString: String = "(" + x + ", " + y + ":" + originatingEdges.size + ")"
}

case class AstarEdge(weight: Int, node: AstarNode)
