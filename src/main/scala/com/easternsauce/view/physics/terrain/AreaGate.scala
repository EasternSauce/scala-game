package com.easternsauce.view.physics.terrain

import com.easternsauce.model.creature.Creature

case class AreaGate(areaFrom: String, fromPosX: Float, fromPosY: Float, areaTo: String, toPosX: Float, toPosY: Float) {

  val width = 1.5f
  val height = 1.5f

  private val bodyFrom: AreaGateBody = AreaGateBody(fromPosX, fromPosY, width, height)
  private val bodyTo: AreaGateBody = AreaGateBody(toPosX, toPosY, width, height)

  def init(terrains: Map[String, Terrain]): Unit = {
    bodyFrom.init(terrains(areaFrom).world)
    bodyTo.init(terrains(areaTo).world)
  }

  def activate(creature: Creature): Unit = {
    println("activated area gate")

    // TODO

//    if (!creature.passedGateRecently) {
//      if (creature.isPlayer) {
//        val (destination: Area, posX: Float, posY: Float) = areaMap(creature.params.areaId.get) match {
//          case `areaFrom` => (areaTo, toPosX, toPosY)
//          case `areaTo`   => (areaFrom, fromPosX, fromPosY)
//          case _          => throw new RuntimeException("should never reach here")
//        }

//        musicManager.stopMusic()
//
//        moveCreature(creature, destination, posX, posY)
//
//        destination.reset()
//
//        if (destination.music.nonEmpty) musicManager.playMusic(destination.music.get, 0.2f)
//
//        currentAreaId = Some(destination.id)

//      }
//    }

  }

  def destroy(): Unit = {
    bodyFrom.b2Body.getWorld.destroyBody(bodyFrom.b2Body)
    bodyTo.b2Body.getWorld.destroyBody(bodyTo.b2Body)
  }

}
