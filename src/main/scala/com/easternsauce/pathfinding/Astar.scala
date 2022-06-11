package com.easternsauce.pathfinding

import com.easternsauce.view.physics.terrain.Terrain
import com.softwaremill.quicklens._

import scala.annotation.tailrec
import scala.collection.immutable.Map
import scala.util.chaining.scalaUtilChainingOps

object Astar {
  def generateGraph(terrain: Terrain): Map[Pos, PathingNode] = {
    val elems =
      (for (x <- 0 until terrain.widthInTiles; y <- 0 until terrain.heightInTiles)
        yield Pos(x, y) -> PathingNode(Pos(x, y)))

    val pathingNodes: Map[Pos, PathingNode] = elems.toMap

    def tryAddingEdge(
      pathingNodes: Map[Pos, PathingNode],
      terrain: Terrain,
      fromX: Int,
      fromY: Int,
      toX: Int,
      toY: Int,
      weight: Float
    ): Map[Pos, PathingNode] = {
      if (0 <= toY && toY < terrain.heightInTiles && 0 <= toX && toX < terrain.widthInTiles) {
        if (terrain.traversable(fromY)(fromX) && terrain.traversable(toY)(toX)) {
          val targetNode = pathingNodes(Pos(toX, toY))
          pathingNodes.updated(Pos(fromX, fromY), pathingNodes(Pos(fromX, fromY)).addEdge(weight, targetNode))
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
                && terrain.traversable(y)(x - 1) && terrain.traversable(y - 1)(x)
              ) tryAddingEdge(pathingNodes, terrain, x, y, x - 1, y - 1, diagonalWeight)
              else pathingNodes
          )
          .pipe(
            pathingNodes =>
              if (
                x + 1 < terrain.widthInTiles && y - 1 >= 0
                && terrain.traversable(y)(x + 1) && terrain.traversable(y - 1)(x)
              ) tryAddingEdge(pathingNodes, terrain, x, y, x + 1, y - 1, diagonalWeight)
              else pathingNodes
          )
          .pipe(
            pathingNodes =>
              if (
                x - 1 >= 0 && y + 1 < terrain.heightInTiles
                && terrain.traversable(y)(x - 1) && terrain.traversable(y + 1)(x)
              ) tryAddingEdge(pathingNodes, terrain, x, y, x - 1, y + 1, diagonalWeight)
              else pathingNodes
          )
          .pipe(
            pathingNodes =>
              if (
                x + 1 < terrain.widthInTiles && y + 1 < terrain.heightInTiles
                && terrain.traversable(y)(x + 1) && terrain.traversable(y + 1)(x)
              ) tryAddingEdge(pathingNodes, terrain, x, y, x + 1, y + 1, diagonalWeight)
              else pathingNodes
          )
    }

  }

  def findPath(pathingGraph: Map[Pos, PathingNode], startPos: Pos, finishPos: Pos): List[Pos] = {
    val freshAstarGraph = Astar
      .getAstarGraph(pathingGraph)
      .modify(_.at(startPos))
      .using(_.modify(_.g).setTo(0))

    val astarState =
      AstarState(
        astarGraph = freshAstarGraph,
        openSet = Set(startPos),
        closedSet = Set(),
        finishPos = finishPos,
        foundPath = false
      )

    @tailrec
    def traverse(astarState: AstarState): AstarState = {
      if (astarState.openSet.nonEmpty && !astarState.foundPath) {
        val currentNode = astarState.astarGraph(astarState.openSet.minBy {
          case Pos(x, y) => astarState.astarGraph(Pos(x, y)).f
        })
        val resultingAstarState = if (currentNode.pos == finishPos) {
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
      originNodePos: Pos,
      connectedNodePos: Pos,
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

    def reconstructPath(lastNode: AstarNode): List[Pos] = {
      if (lastNode.parent.nonEmpty) {
        lastNode.pos :: reconstructPath(result.astarGraph(lastNode.parent.get))
      } else List()
    }

    reconstructPath(lastNode).reverse
  }

  def getAstarGraph(pathingGraph: Map[Pos, PathingNode]): Map[Pos, AstarNode] = {
    pathingGraph.view.mapValues(AstarNode(_)).toMap
  }

  def calculateHeuristic(startPos: Pos, finishPos: Pos): Double = {
    (Math.abs(finishPos.x - startPos.x) + Math.abs(finishPos.y - startPos.y)) * 10
  }

}

case class PathingNode(pos: Pos, outgoingEdges: List[PathingEdge] = List()) {
  def addEdge(weight: Float, node: PathingNode): PathingNode = {
    val newEdge = PathingEdge(weight, node.pos)
    PathingNode(pos, newEdge :: outgoingEdges)
  }

  override def toString: String = "(" + pos.x + ", " + pos.y + ":" + outgoingEdges.size + ")"
}

case class PathingEdge(weight: Float, connectedNodePos: Pos)

case class AstarNode(
  pathingNode: PathingNode,
  parent: Option[Pos] = None,
  f: Double = Double.MaxValue,
  g: Double = Double.MaxValue,
  h: Double = Double.MaxValue
) {
  def pos: Pos = pathingNode.pos
}

case class AstarState(
  astarGraph: Map[Pos, AstarNode],
  openSet: Set[Pos],
  closedSet: Set[Pos],
  finishPos: Pos,
  foundPath: Boolean
)

case class Pos(x: Int, y: Int)
