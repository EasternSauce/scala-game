package com.easternsauce.model

import com.badlogic.gdx.math.Vector2
import com.easternsauce.model.creature.ability.AbilityState
import com.easternsauce.model.event.{ComponentCreateBodyEvent, ComponentDestroyBodyEvent}
import com.easternsauce.view.physics.PhysicsController
import com.softwaremill.quicklens._

import scala.util.chaining._

trait AbilityInteractions {
  this: GameState =>

  def onAbilityComponentActiveStart(creatureId: String, abilityId: String, componentId: String): GameState = {

    val ability = this.abilities(creatureId, abilityId)

    val creature = this.creatures(creatureId)

    this
      .pipe(
        gameState =>
          gameState
            .modify(_.events)
            .setTo(ComponentCreateBodyEvent(creatureId, abilityId, componentId) :: gameState.events)
      )
      .modifyGameStateAbilityComponent(creatureId, abilityId, componentId) {
        _.modify(_.params.abilityActiveAnimationTimer)
          .using(_.restart())
          .modify(_.params.activeTimer)
          .using(_.restart())
          .modify(_.params.channelTimer)
          .using(_.stop())
          .modify(_.params.abilityChannelAnimationTimer)
          .using(_.stop())
          //.setDirVector(Vector2Wrapper(creature.params.dirVector.x, creature.params.dirVector.y)) TODO: this is too late to set this
          .pipe(ability.updateComponentHitbox(creature, _))
      }
  }

  def onAbilityComponentChannelStart(creatureId: String, abilityId: String, componentId: String): GameState = {

    val ability = this.abilities(creatureId, abilityId)
    val creature = this.creatures(creatureId)

    this
      .modifyGameStateAbilityComponent(creatureId, abilityId, componentId) {
        _.modify(_.params.channelTimer)
          .using(_.restart())
          .modify(_.params.abilityChannelAnimationTimer)
          .using(_.restart())
          .pipe(ability.updateComponentHitbox(creature, _))
      }

  }

  def onAbilityComponentInactiveStart(creatureId: String, abilityId: String, componentId: String): GameState = {

    this
      .pipe(
        gameState =>
          gameState
            .modify(_.events)
            .setTo(ComponentDestroyBodyEvent(creatureId, abilityId, componentId) :: gameState.events)
      )
      .modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(
        _.modify(_.params.activeTimer)
          .using(_.stop())
          .modify(_.params.abilityActiveAnimationTimer)
          .using(_.stop())
      )

  }

  def onAbilityComponentChannelUpdate(creatureId: String, abilityId: String, componentId: String): GameState = {
    val ability = abilities(creatureId, abilityId)
    val creature = this.creatures(creatureId)

    this.modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(ability.updateComponentHitbox(creature, _))
  }

  def onAbilityComponentActiveUpdate(creatureId: String, abilityId: String, componentId: String): GameState = {
    val ability = abilities(creatureId, abilityId)
    val creature = this.creatures(creatureId)

    this.modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(ability.updateComponentHitbox(creature, _))

  }

  def updateCreatureAbility(
    physicsController: PhysicsController,
    creatureId: String,
    abilityId: String,
    delta: Float
  ): GameState = {
    val ability = abilities(creatureId, abilityId)

    ability.components.keys
      .foldLeft(this)((gameState, componentId) => {
        val component = ability.components(componentId)

        val channelTimer = component.params.channelTimer
        val activeTimer = component.params.activeTimer

        import com.easternsauce.model.creature.ability.AbilityState._
        (component.params.state match {
          case DelayedStart =>
            gameState.pipe {
              case gameState if ability.params.abilityTimer.time > component.params.delay =>
                gameState
                  .onAbilityComponentChannelStart(creatureId, abilityId, componentId)
                  .modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(
                    _.modify(_.params.state).setTo(AbilityState.Channel)
                  )
              case gameState => gameState
            }
          case Channel =>
            gameState
              .pipe {
                case gameState if channelTimer.time > component.specification.totalChannelTime =>
                  gameState
                    .modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(_.stop().makeActive())
                    .onAbilityComponentActiveStart(creatureId, abilityId, componentId)
                case gameState => gameState
              }
              .onAbilityComponentChannelUpdate(creatureId, abilityId, componentId)
          case Active =>
            val pos =
              if (
                physicsController.entityBodies.contains(creatureId) && physicsController
                  .entityBodies(creatureId)
                  .componentBodies
                  .contains(abilityId -> componentId)
              ) {
                physicsController.entityBodies(creatureId).componentBodies(abilityId -> componentId).pos
              } else
                new Vector2(component.params.abilityHitbox.x, component.params.abilityHitbox.y)

            gameState
              .pipe {
                case state if activeTimer.time > component.specification.totalActiveTime =>
                  state
                    .modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(_.stop().makeInactive())
                    .onAbilityComponentInactiveStart(creatureId, abilityId, componentId)

                case state => state
              }
              .onAbilityComponentActiveUpdate(creatureId, abilityId, componentId)
              .modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(
                _.modify(_.params.abilityHitbox.x)
                  .setTo(pos.x)
                  .modify(_.params.abilityHitbox.y)
                  .setTo(pos.y)
              )
          case Inactive =>
            gameState // TODO do we need update anything when inactive?
          case _ => gameState
        }).modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(ability.updateComponentTimers(_, delta))
      })
      .modifyGameStateAbility(creatureId, abilityId)(_.updateTimers(delta))

  }

  def performAbility(creatureId: String, abilityId: String): GameState = {
    val creature = creatures(creatureId)

    val ability = creature.params.abilities(abilityId)

    if (
      creature.params.stamina > 0 && !ability.componentsActive && !ability.onCooldown
      /*&& !creature.abilityActive*/
    ) {
      ability.components.keys
        .foldLeft(this)((gameState, componentId) => {
          gameState
            .modifyGameStateAbilityComponent(creatureId, abilityId, componentId) {
              _.modify(_.params.channelTimer)
                .using(_.restart())
                .modify(_.params.state)
                .setTo(AbilityState.DelayedStart)
            }
        })
        .modifyGameStateCreature(creatureId)(
          _.modify(_.params.staminaRegenerationDisabledTimer)
            .using(_.restart())
            .modify(_.params.isStaminaRegenerationDisabled)
            .setTo(true)
            .takeStaminaDamage(15f)
        )
        .modifyGameStateAbility(creatureId, abilityId) {
          _.onStart(this, creatureId, abilityId)
            .modify(_.params.abilityTimer)
            .using(_.restart())
        }

    } else this
  }
}
