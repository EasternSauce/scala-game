package com.easternsauce.pathfinding

import com.easternsauce.view.physics.terrain.Terrain

import scala.util.chaining.scalaUtilChainingOps

object Astar {
  def generateGraph(terrain: Terrain): Map[(Int, Int), AstarNode] = {
    val astarNodes = (for (x <- 0 until terrain.widthInTiles; y <- 0 until terrain.heightInTiles)
      yield (x, y) -> AstarNode(x, y)).toMap

    def tryAddingEdge(terrain: Terrain, node: AstarNode, x: Int, y: Int, weight: Int): Map[(Int, Int), AstarNode] = {
      if (0 <= y && y < terrain.heightInTiles && 0 <= x && x < terrain.widthInTiles) {
        if (terrain.traversable(y)(x)) {
          val targetNode = astarNodes(x, y)
          astarNodes.updated((node.x, node.y), node.addEdge(weight, targetNode))
        } else astarNodes
      } else astarNodes
    }

    val straightWeight = 10
    val diagonalWeight = 14

    (for {
      x <- 0 until terrain.widthInTiles
      y <- 0 until terrain.heightInTiles
    } yield (x, y)).foldLeft(astarNodes) {
      case (astarNodes, (x, y)) =>
        astarNodes
          .pipe(astarNodes => tryAddingEdge(terrain, astarNodes((x, y)), x - 1, y, straightWeight))
          .pipe(astarNodes => tryAddingEdge(terrain, astarNodes((x, y)), x + 1, y, straightWeight))
          .pipe(astarNodes => tryAddingEdge(terrain, astarNodes((x, y)), x, y - 1, straightWeight))
          .pipe(astarNodes => tryAddingEdge(terrain, astarNodes((x, y)), x, y + 1, straightWeight))
          .pipe(
            astarNodes =>
              if (
                x - 1 >= 0 && y - 1 >= 0
                && terrain.traversable(y)(x - 1) && terrain.traversable(y - 1)(x)
              ) tryAddingEdge(terrain, astarNodes((x, y)), x - 1, y - 1, diagonalWeight)
              else astarNodes
          )
          .pipe(
            astarNodes =>
              if (
                x + 1 >= 0 && y - 1 >= 0
                && terrain.traversable(y)(x + 1) && terrain.traversable(y - 1)(x)
              ) tryAddingEdge(terrain, astarNodes((x, y)), x + 1, y - 1, diagonalWeight)
              else astarNodes
          )
          .pipe(
            astarNodes =>
              if (
                x - 1 >= 0 && y + 1 >= 0
                && terrain.traversable(y)(x - 1) && terrain.traversable(y + 1)(x)
              ) tryAddingEdge(terrain, astarNodes((x, y)), x - 1, y + 1, diagonalWeight)
              else astarNodes
          )
          .pipe(
            astarNodes =>
              if (
                x + 1 >= 0 && y + 1 >= 0
                && terrain.traversable(y)(x + 1) && terrain.traversable(y + 1)(x)
              ) tryAddingEdge(terrain, astarNodes((x, y)), x + 1, y + 1, diagonalWeight)
              else astarNodes
          )
    }
  }

}

case class AstarNode(
  x: Int,
  y: Int,
  parent: Option[AstarNode] = None,
  neighbors: List[AStarEdge] = List(),
  f: Double = Double.MaxValue,
  g: Double = Double.MaxValue
) {

  def addEdge(weight: Int, node: AstarNode): AstarNode = {
    val newEdge = AStarEdge(weight, node)
    AstarNode(x, y, parent, neighbors ++ List(newEdge), f, g)
  }
  def calculateHeuristic(target: AstarNode): Double = {
    (Math.abs(target.x - x) + Math.abs(target.y - y)) * 10
  }
}

case class AStarEdge(weight: Int, node: AstarNode)
