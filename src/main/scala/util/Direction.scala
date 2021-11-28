package util


import scala.util.Random

object Direction extends Enumeration {

  def isHorizontal(value: Direction.Value): Boolean = {
    value match {
      case Left  => true
      case Right => true
      case _     => false
    }
  }

  def isVertical(value: Direction.Value): Boolean = {
    value match {
      case Up   => true
      case Down => true
      case _    => false
    }
  }

  type Direction = Value
  val Left, Right, Up, Down = Value

  def randomDir(randomGenerator: Random): Direction = {
    randomGenerator.nextInt(4) match {
      case 0 => Left
      case 1 => Right
      case 2 => Up
      case 3 => Down
    }
  }
}
