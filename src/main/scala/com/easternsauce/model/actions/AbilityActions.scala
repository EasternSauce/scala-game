package com.easternsauce.model.actions

import com.badlogic.gdx.math.Vector2
import com.easternsauce.model.GameState
import com.easternsauce.model.creature.ability.{Ability, AbilityComponent, AbilityState}
import com.easternsauce.model.event.{PlaySoundEvent, UpdatePhysicsOnComponentCreateBodyEvent, UpdatePhysicsOnComponentDestroyBodyEvent}
import com.easternsauce.view.physics.PhysicsController
import com.softwaremill.quicklens._

import scala.util.chaining.scalaUtilChainingOps

trait AbilityActions {
  this: GameState =>

  def onAbilityComponentActiveStart(creatureId: String, abilityId: String, componentId: String): GameState = {

    val ability = this.abilities(creatureId, abilityId)

    val creature = this.creatures(creatureId)

    this
      .pipe(
        gameState =>
          gameState
            .modify(_.events)
            .setTo(UpdatePhysicsOnComponentCreateBodyEvent(creatureId, abilityId, componentId) :: gameState.events)
      )
      .pipe(
        gameState =>
          gameState
            .modify(_.events)
            .setToIf(ability.abilityActiveSoundId.nonEmpty)(
              PlaySoundEvent(ability.abilityActiveSoundId.get) :: gameState.events
            )
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
          .pipe(ability.updateComponentHitbox(creature, _))
          .modify(_.params.forceStopped)
          .setTo(false)
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
            .setTo(UpdatePhysicsOnComponentDestroyBodyEvent(creatureId, abilityId, componentId) :: gameState.events)
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

    this
      .modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(ability.updateComponentHitbox(creature, _))
      .modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(ability.updateRenderPos(creature, _))
  }

  def onAbilityComponentActiveUpdate(creatureId: String, abilityId: String, componentId: String): GameState = {
    val ability = abilities(creatureId, abilityId)
    val creature = this.creatures(creatureId)

    this
      .modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(ability.updateComponentHitbox(creature, _))
      .modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(ability.updateRenderPos(creature, _))
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
                case gameState
                    if channelTimer.time > component.specification.totalChannelTime / component.params.speed =>
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
                case state
                    if activeTimer.time > component.specification.totalActiveTime / component.params.speed || component.params.forceStopped =>
                  state
                    .modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(_.stop().makeInactive())
                    .onAbilityComponentInactiveStart(creatureId, abilityId, componentId)

                case state => state
              }
              // set hitbox x and y to body x and y
              .modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(
                _.modify(_.params.abilityHitbox.x)
                  .setTo(pos.x)
                  .modify(_.params.abilityHitbox.y)
                  .setTo(pos.y)
              )
              .onAbilityComponentActiveUpdate(
                creatureId,
                abilityId,
                componentId
              ) // needs to be after setting hitbox so it everrides properly
          case _ => gameState
        }).modifyGameStateAbilityComponent(creatureId, abilityId, componentId)(ability.updateComponentTimers(_, delta))
      })
      .modifyGameStateAbility(creatureId, abilityId)(_.updateTimers(delta))

  }

  def onAbilityComponentCollision(creatureId: String, abilityId: String, componentId: String): GameState = {
    if (abilities(creatureId, abilityId).isDestroyOnCollision) {
      this.modifyGameStateAbility(creatureId, abilityId)(
        _.onCollision().modify(_.components.at(componentId)).using(_.forceStop())
      )
    } else {
      this.modifyGameStateAbility(creatureId, abilityId)(_.onCollision())
    }
  }

  def performAbility(creatureId: String, abilityId: String): GameState = {
    val ability = this.abilities(creatureId, abilityId)
    val creature = this.creatures(creatureId)

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
        .modifyGameStateCreature(creatureId) {
          _.modify(_.params.staminaRegenerationDisabledTimer)
            .using(_.restart())
            .modify(_.params.isStaminaRegenerationDisabled)
            .setTo(true)
            .takeStaminaDamage(15f)
            .modifyAbility(abilityId) {
              _.modify(_.params.abilityTimer)
                .using(_.restart())
            }
        }
        .pipe(gameState => creature.performAbility(gameState, abilityId))

    } else this
  }

  def modifyEachAbilityComponent(creatureId: String, abilityId: String)(modification: (Ability, String) => Ability): GameState = {
    this.modifyGameStateAbility(creatureId, abilityId) { ability =>
      ability.components.keys
        .foldLeft(ability)(modification)
    }
  }
}
