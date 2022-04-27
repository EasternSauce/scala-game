package com.easternsauce.system

import scala.util.{Random => ScalaRandom}

object Random {
  val randomGenerator: ScalaRandom = new ScalaRandom()

  def between(minInclusive: Float, maxExclusive: Float): Float = {
    randomGenerator.between(minInclusive, maxExclusive)
  }

  def nextInt(): Int = randomGenerator.nextInt()
}
