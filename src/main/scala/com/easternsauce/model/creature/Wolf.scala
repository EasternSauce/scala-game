package com.easternsauce.model.creature

import com.easternsauce.model.creature.ability.Ability
import com.easternsauce.model.creature.ability.magic.{BubbleAbility, IceSpearAbility}
import com.easternsauce.model.creature.ability.sword.SwingWeaponAbility
import com.easternsauce.util.Direction
import com.easternsauce.util.Direction.Direction

case class Wolf(override val params: CreatureParams) extends Enemy(params = params) {
  override val spriteType: String = "wolf2"
  override val textureWidth: Int = 32
  override val textureHeight: Int = 34
  override val width: Float = 2.85f
  override val height: Float = 2.85f
  override val frameDuration: Float = 0.1f
  override val frameCount: Int = 6
  override val neutralStanceFrame: Int = 1
  override val dirMap: Map[Direction, Int] =
    Map(Direction.Up -> 3, Direction.Down -> 0, Direction.Left -> 1, Direction.Right -> 2)
  override val baseLife: Float = 150f

  override val onGettingHitSoundId: Option[String] = Some("dogWhine")

  override val abilityUsages: Map[String, AbilityUsage] =
    Map(
      "iceSpear" -> AbilityUsage(weight = 80f, minimumDistance = 8f),
      "bubble" -> AbilityUsage(weight = 20f, minimumDistance = 12f)
    )

  override val abilities: List[Ability] = List(SwingWeaponAbility(), IceSpearAbility(), BubbleAbility())

  override val dropTable = Map(
    "ringmailGreaves" -> 0.1f,
    "leatherArmor" -> 0.05f,
    "hideGloves" -> 0.1f,
    "leatherHelmet" -> 0.1f,
    "healingPowder" -> 0.5f
  )

  override def update(delta: Float): Wolf = {
    super.update(delta).asInstanceOf[Wolf]
  }

  override def copy(params: CreatureParams): Wolf = {
    Wolf(params)
  }
}
