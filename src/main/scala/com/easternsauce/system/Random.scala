package com.easternsauce.system

import scala.util.Random

object Random {
  val randomGenerator: Random = new Random()

  def between(minInclusive: Float, maxExclusive: Float): Float = {
    randomGenerator.between(minInclusive, maxExclusive)
  }
}
