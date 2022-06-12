package com.easternsauce.view.pathfinding

import com.easternsauce.util.Vector2Wrapper
import com.easternsauce.view.physics.terrain.Terrain
import com.softwaremill.quicklens._

import scala.annotation.tailrec
import scala.collection.immutable.Map
import scala.util.chaining.scalaUtilChainingOps

object Astar {
  def generatePathingGraph(terrain: Terrain): Map[Vector2Wrapper, PathingNode] = {
    val elems =
      (for (x <- 0 until terrain.widthInTiles; y <- 0 until terrain.heightInTiles)
        yield Vector2Wrapper(x, y) -> PathingNode(Vector2Wrapper(x, y)))

    val pathingNodes: Map[Vector2Wrapper, PathingNode] = elems.toMap

    def tryAddingEdge(
      pathingNodes: Map[Vector2Wrapper, PathingNode],
      terrain: Terrain,
      fromX: Int,
      fromY: Int,
      toX: Int,
      toY: Int,
      weight: Float
    ): Map[Vector2Wrapper, PathingNode] = {
      if (0 <= toY && toY < terrain.heightInTiles && 0 <= toX && toX < terrain.widthInTiles) {
        if (terrain.traversable(Vector2Wrapper(fromX, fromY)) && terrain.traversable(Vector2Wrapper(toX, toY))) {
          val targetNode = pathingNodes(Vector2Wrapper(toX, toY))
          pathingNodes.updated(
            Vector2Wrapper(fromX, fromY),
            pathingNodes(Vector2Wrapper(fromX, fromY)).addEdge(weight, targetNode)
          )
        } else pathingNodes
      } else pathingNodes
    }

    val straightWeight = 10f
    val diagonalWeight = 14.1421356237f

    (for {
      x <- 0 until terrain.widthInTiles
      y <- 0 until terrain.heightInTiles
    } yield (x, y)).foldLeft(pathingNodes) {
      case (pathingNodes, (x, y)) =>
        pathingNodes
          .pipe(tryAddingEdge(_, terrain, x, y, x - 1, y, straightWeight))
          .pipe(tryAddingEdge(_, terrain, x, y, x + 1, y, straightWeight))
          .pipe(tryAddingEdge(_, terrain, x, y, x, y - 1, straightWeight))
          .pipe(tryAddingEdge(_, terrain, x, y, x, y + 1, straightWeight))
          .pipe(
            pathingNodes =>
              if (
                x - 1 >= 0 && y - 1 >= 0
                && terrain.traversable(Vector2Wrapper(x - 1,y)) && terrain.traversable(Vector2Wrapper(x, y - 1))
              ) tryAddingEdge(pathingNodes, terrain, x, y, x - 1, y - 1, diagonalWeight)
              else pathingNodes
          )
          .pipe(
            pathingNodes =>
              if (
                x + 1 < terrain.widthInTiles && y - 1 >= 0
                && terrain.traversable(Vector2Wrapper(x + 1,y)) && terrain.traversable(Vector2Wrapper(x,y - 1))
              ) tryAddingEdge(pathingNodes, terrain, x, y, x + 1, y - 1, diagonalWeight)
              else pathingNodes
          )
          .pipe(
            pathingNodes =>
              if (
                x - 1 >= 0 && y + 1 < terrain.heightInTiles
                && terrain.traversable(Vector2Wrapper(x - 1, y)) && terrain.traversable(Vector2Wrapper(x, y + 1))
              ) tryAddingEdge(pathingNodes, terrain, x, y, x - 1, y + 1, diagonalWeight)
              else pathingNodes
          )
          .pipe(
            pathingNodes =>
              if (
                x + 1 < terrain.widthInTiles && y + 1 < terrain.heightInTiles
                && terrain.traversable(Vector2Wrapper(x + 1,y)) && terrain.traversable(Vector2Wrapper(x, y + 1))
              ) tryAddingEdge(pathingNodes, terrain, x, y, x + 1, y + 1, diagonalWeight)
              else pathingNodes
          )
    }

  }

  // caution: heavy computational load!
  def findPath(terrain: Terrain, startPos: Vector2Wrapper, finishPos: Vector2Wrapper): List[Vector2Wrapper] = {
    val startTilePos = terrain.getClosestTile(startPos)
    val finishTilePos = terrain.getClosestTile(finishPos)

    val freshAstarGraph = Astar
      .getAstarGraph(terrain.pathingGraph)
      .modify(_.at(startTilePos))
      .using(_.modify(_.g).setTo(0))

    val astarState =
      AstarState(
        astarGraph = freshAstarGraph,
        openSet = Set(startTilePos),
        closedSet = Set(),
        finishPos = finishTilePos,
        foundPath = false
      )

    @tailrec
    def traverse(astarState: AstarState): AstarState = {
      if (astarState.openSet.nonEmpty && !astarState.foundPath) {
        val currentNode = astarState.astarGraph(astarState.openSet.minBy {
          case Vector2Wrapper(x, y) => astarState.astarGraph(Vector2Wrapper(x, y)).f
        })
        val resultingAstarState = if (currentNode.pos == finishTilePos) {
          astarState.modify(_.foundPath).setTo(true)
        } else {
          val updatedAstarState = astarState
            .modify(_.openSet)
            .setTo(astarState.openSet - currentNode.pos)
            .modify(_.closedSet)
            .setTo(astarState.closedSet + currentNode.pos)
          currentNode.pathingNode.outgoingEdges
            .foldLeft(updatedAstarState) {
              case (astarState, PathingEdge(weight, connectedNodePos)) =>
                processConnectedNode(astarState, currentNode.pos, connectedNodePos, weight)
            }
        }

        traverse(resultingAstarState)
      } else {
        astarState
      }
    }

    def processConnectedNode(
      astarState: AstarState,
      originNodePos: Vector2Wrapper,
      connectedNodePos: Vector2Wrapper,
      distanceBetweenNodes: Float
    ): AstarState = {
      if (astarState.closedSet.contains(connectedNodePos)) {
        astarState
      } else {
        val originNode = astarState.astarGraph(originNodePos)
        val connectedNode = astarState.astarGraph(connectedNodePos)

        val tentativeGscore = originNode.g + distanceBetweenNodes
        connectedNode match {
          case node if !astarState.openSet.contains(node.pos) =>
            val updatedNode = node
              .modify(_.h)
              .setTo(Astar.calculateHeuristic(node.pos, astarState.finishPos))
              .modify(_.parent)
              .setTo(Some(originNode.pos))
              .modify(_.g)
              .setTo(tentativeGscore)
              .pipe(node => node.modify(_.f).setTo(node.g + node.h))

            astarState
              .modify(_.astarGraph)
              .using(_.updated(node.pos, updatedNode))
              .modify(_.openSet)
              .setTo(astarState.openSet + node.pos)
          case node if tentativeGscore < node.g =>
            val updatedNode = node
              .modify(_.parent)
              .setTo(Some(originNode.pos))
              .modify(_.g)
              .setTo(tentativeGscore)
              .pipe(node => node.modify(_.f).setTo(node.g + node.h))

            astarState.modify(_.astarGraph).setTo(astarState.astarGraph.updated(node.pos, updatedNode))
          case _ => astarState
        }
      }
    }

    val result = traverse(astarState)

    val lastNode = result.astarGraph(result.finishPos)

    def reconstructPath(lastNode: AstarNode): List[Vector2Wrapper] = {
      if (lastNode.parent.nonEmpty) {
        lastNode.pos :: reconstructPath(result.astarGraph(lastNode.parent.get))
      } else List()
    }

    reconstructPath(lastNode).reverse.map(terrain.getTileCenter)
  }

  def getAstarGraph(pathingGraph: Map[Vector2Wrapper, PathingNode]): Map[Vector2Wrapper, AstarNode] = {
    pathingGraph.view.mapValues(AstarNode(_)).toMap
  }

  def calculateHeuristic(startPos: Vector2Wrapper, finishPos: Vector2Wrapper): Double = {
    (Math.abs(finishPos.x - startPos.x) + Math.abs(finishPos.y - startPos.y)) * 10
  }

}

case class PathingNode(pos: Vector2Wrapper, outgoingEdges: List[PathingEdge] = List()) {
  def addEdge(weight: Float, node: PathingNode): PathingNode = {
    val newEdge = PathingEdge(weight, node.pos)
    PathingNode(pos, newEdge :: outgoingEdges)
  }

  override def toString: String = "(" + pos.x + ", " + pos.y + ":" + outgoingEdges.size + ")"
}

case class PathingEdge(weight: Float, connectedNodePos: Vector2Wrapper)

case class AstarNode(
  pathingNode: PathingNode,
  parent: Option[Vector2Wrapper] = None,
  f: Double = Double.MaxValue,
  g: Double = Double.MaxValue,
  h: Double = Double.MaxValue
) {
  def pos: Vector2Wrapper = pathingNode.pos
}

case class AstarState(
  astarGraph: Map[Vector2Wrapper, AstarNode],
  openSet: Set[Vector2Wrapper],
  closedSet: Set[Vector2Wrapper],
  finishPos: Vector2Wrapper,
  foundPath: Boolean
)
