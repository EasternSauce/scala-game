package com.easternsauce.model.creature.ability.magic

import com.easternsauce.model.GameState
import com.easternsauce.model.creature.ability.{Ability, AbilityComponent, AbilityParams, AbilitySpecification}
import com.easternsauce.model.util.EnhancedChainingSyntax.enhancedScalaUtilChainingOps
import com.softwaremill.quicklens.ModifyPimp

case class DashAbility(
  override val params: AbilityParams = AbilityParams(id = "dash"),
  override val components: Map[String, AbilityComponent] = Map()
) extends Ability(params = params, components = components) {
  override val specification: Option[AbilitySpecification] = None

  override val cooldownTime: Float = 0.5f

  override def onStart(creatureId: String): GameState => GameState = { gameState =>
    val creature = gameState.creatures(creatureId)
    gameState.pipeIf(creature.ableToMove) {
      _.modifyGameStateCreature(creatureId) {
        _.activateEffect("dash", 0.15f)
          .modify(_.params.dashDir)
          .setTo(creature.params.actionDirVector)
          .modify(_.params.dashVelocity)
          .setTo(70f)
      }
    }
  }

  def copy(params: AbilityParams = params, components: Map[String, AbilityComponent] = components): DashAbility =
    DashAbility(params, components)
}
