package com.easternsauce.json

import com.easternsauce.model.GameState
import com.easternsauce.model.area.loot.LootPile
import com.easternsauce.model.area.{Area, AreaParams, EnemySpawnPoint}
import com.easternsauce.model.creature._
import com.easternsauce.model.creature.ability.AbilityState.AbilityState
import com.easternsauce.model.creature.ability.ComponentType.ComponentType
import com.easternsauce.model.creature.ability._
import com.easternsauce.model.creature.ability.bow.BowShotAbility
import com.easternsauce.model.creature.ability.magic._
import com.easternsauce.model.creature.ability.sword.{SwingWeaponAbility, ThrustWeaponAbility}
import com.easternsauce.model.creature.effect.Effect
import com.easternsauce.model.event.UpdateEvent
import com.easternsauce.model.hud.{InventoryWindow, LootPilePickupMenu}
import com.easternsauce.model.item.{Item, ItemParameterValue, ItemTemplate}
import com.easternsauce.model.util.SimpleTimer
import com.easternsauce.util.Direction.Direction
import com.easternsauce.util.{Direction, Vec2}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}

object JsonCodecs {
  implicit val decodeGameState: Decoder[GameState] = deriveDecoder
  implicit val encodeGameState: Encoder[GameState] = deriveEncoder
  implicit val decodeArea: Decoder[Area] = deriveDecoder
  implicit val encodeArea: Encoder[Area] = deriveEncoder
  implicit val decodeUpdateEvent: Decoder[UpdateEvent] = deriveDecoder
  implicit val encodeUpdateEvent: Encoder[UpdateEvent] = deriveEncoder
  implicit val decodeInventoryState: Decoder[InventoryWindow] = deriveDecoder
  implicit val encodeInventoryState: Encoder[InventoryWindow] = deriveEncoder
  implicit val decodeCreatureParams: Decoder[CreatureParams] = deriveDecoder
  implicit val encodeCreatureParams: Encoder[CreatureParams] = deriveEncoder
  implicit val decodeSimpleTimer: Decoder[SimpleTimer] = deriveDecoder
  implicit val encodeSimpleTimer: Encoder[SimpleTimer] = deriveEncoder
  implicit val decodeItem: Decoder[Item] = deriveDecoder
  implicit val encodeItem: Encoder[Item] = deriveEncoder
  implicit val decodeItemTemplate: Decoder[ItemTemplate] = deriveDecoder
  implicit val encodeItemTemplate: Encoder[ItemTemplate] = deriveEncoder
  implicit val decodeItemParameterValue: Decoder[ItemParameterValue] = deriveDecoder
  implicit val encodeItemParameterValue: Encoder[ItemParameterValue] = deriveEncoder
  implicit val decodeAbilityParams: Decoder[AbilityParams] = deriveDecoder
  implicit val encodeAbilityParams: Encoder[AbilityParams] = deriveEncoder
  implicit val decodeAbilityComponentParams: Decoder[ComponentParams] = deriveDecoder
  implicit val encodeAbilityComponentParams: Encoder[ComponentParams] = deriveEncoder
  implicit val decodeAbilitySpecification: Decoder[AbilitySpecification] = deriveDecoder
  implicit val encodeAbilitySpecification: Encoder[AbilitySpecification] = deriveEncoder
  implicit val decodeComponentType: Decoder[ComponentType] = Decoder.decodeEnumeration(ComponentType)
  implicit val encodeComponentType: Encoder[ComponentType] = Encoder.encodeEnumeration(ComponentType)
  implicit val decodeAbilityComponent: Decoder[AbilityComponent] = deriveDecoder
  implicit val encodeAbilityComponent: Encoder[AbilityComponent] = deriveEncoder
  implicit val decodeAbilityHitbox: Decoder[AbilityHitbox] = deriveDecoder
  implicit val encodeAbilityHitbox: Encoder[AbilityHitbox] = deriveEncoder
  implicit val decodeAbilityState: Decoder[AbilityState] = Decoder.decodeEnumeration(AbilityState)
  implicit val encodeAbilityState: Encoder[AbilityState] = Encoder.encodeEnumeration(AbilityState)
  implicit val decodeVector2Wrapper: Decoder[Vec2] = deriveDecoder
  implicit val encodeVector2Wrapper: Encoder[Vec2] = deriveEncoder
  implicit val decodeDirection: Decoder[Direction] = Decoder.decodeEnumeration(Direction)
  implicit val encodeDirection: Encoder[Direction] = Encoder.encodeEnumeration(Direction)
  implicit val decodeSkeleton: Decoder[Skeleton] = deriveDecoder
  implicit val encodeSkeleton: Encoder[Skeleton] = deriveEncoder
  implicit val decodeWolf: Decoder[Wolf] = deriveDecoder
  implicit val encodeWolf: Encoder[Wolf] = deriveEncoder
  implicit val decodeFireDemon: Decoder[FireDemon] = deriveDecoder
  implicit val encodeFireDemon: Encoder[FireDemon] = deriveEncoder
  implicit val decodeGhost: Decoder[Ghost] = deriveDecoder
  implicit val encodeGhost: Encoder[Ghost] = deriveEncoder
  implicit val decodeGoblin: Decoder[Goblin] = deriveDecoder
  implicit val encodeGoblin: Encoder[Goblin] = deriveEncoder
  implicit val decodeSerpent: Decoder[Serpent] = deriveDecoder
  implicit val encodeSerpent: Encoder[Serpent] = deriveEncoder
  implicit val decodePlayer: Decoder[Player] = deriveDecoder
  implicit val encodePlayer: Encoder[Player] = deriveEncoder
  implicit val decodeSwingWeaponAbility: Decoder[SwingWeaponAbility] = deriveDecoder
  implicit val encodeSwingWeaponAbility: Encoder[SwingWeaponAbility] = deriveEncoder
  implicit val decodeMeteorRainAbility: Decoder[MeteorRainAbility] = deriveDecoder
  implicit val encodeMeteorRainAbility: Encoder[MeteorRainAbility] = deriveEncoder
  implicit val decodeIceSpearAbility: Decoder[IceSpearAbility] = deriveDecoder
  implicit val encodeIceSpearAbility: Encoder[IceSpearAbility] = deriveEncoder
  implicit val decodeBubbleAbility: Decoder[BubbleAbility] = deriveDecoder
  implicit val encodeBubbleAbility: Encoder[BubbleAbility] = deriveEncoder
  implicit val decodeFistSlamAbility: Decoder[FistSlamAbility] = deriveDecoder
  implicit val encodeFistSlamAbility: Encoder[FistSlamAbility] = deriveEncoder
  implicit val decodeMeteorCrashAbility: Decoder[MeteorCrashAbility] = deriveDecoder
  implicit val encodeMeteorCrashAbility: Encoder[MeteorCrashAbility] = deriveEncoder
  implicit val decodeEffect: Decoder[Effect] = deriveDecoder
  implicit val encodeEffect: Encoder[Effect] = deriveEncoder
  implicit val decodeThrustWeaponAbility: Decoder[ThrustWeaponAbility] = deriveDecoder
  implicit val encodeThrustWeaponAbility: Encoder[ThrustWeaponAbility] = deriveEncoder
  implicit val decodeEnemySpawnPoint: Decoder[EnemySpawnPoint] = deriveDecoder
  implicit val encodeEnemySpawnPoint: Encoder[EnemySpawnPoint] = deriveEncoder
  implicit val decodeAreaParams: Decoder[AreaParams] = deriveDecoder
  implicit val encodeAreaParams: Encoder[AreaParams] = deriveEncoder
  implicit val decodeLootPile: Decoder[LootPile] = deriveDecoder
  implicit val encodeLootPile: Encoder[LootPile] = deriveEncoder
  implicit val decodeLootPilePickupMenu: Decoder[LootPilePickupMenu] = deriveDecoder
  implicit val encodeLootPilePickupMenu: Encoder[LootPilePickupMenu] = deriveEncoder
  implicit val decodeDashAbility: Decoder[DashAbility] = deriveDecoder
  implicit val encodeDashAbility: Encoder[DashAbility] = deriveEncoder
  implicit val decodeBowShotAbility: Decoder[BowShotAbility] = deriveDecoder
  implicit val encodeBowShotAbility: Encoder[BowShotAbility] = deriveEncoder

  implicit val encodeCreature: Encoder[Creature] = Encoder.instance { c =>
    {
      c match {
        case v: Skeleton =>
          Map("Skeleton" -> v).asJson
        case v: Wolf =>
          Map("Wolf" -> v).asJson
        case v: FireDemon =>
          Map("FireDemon" -> v).asJson
        case v: Player =>
          Map("Player" -> v).asJson
        case v: Ghost =>
          Map("Ghost" -> v).asJson
        case v: Goblin =>
          Map("Goblin" -> v).asJson
        case v: Serpent =>
          Map("Serpent" -> v).asJson
      }
    }
  }

  implicit val decodeCreature: Decoder[Creature] = Decoder.instance(c => {
    val fname = c.keys.flatMap(_.headOption).toSeq.head
    fname match {
      case "Skeleton"  => c.downField(fname).as[Skeleton]
      case "Wolf"      => c.downField(fname).as[Wolf]
      case "FireDemon" => c.downField(fname).as[FireDemon]
      case "Player"    => c.downField(fname).as[Player]
      case "Ghost"     => c.downField(fname).as[Ghost]
      case "Goblin"    => c.downField(fname).as[Goblin]
      case "Serpent"   => c.downField(fname).as[Serpent]
    }
  })

  implicit val encodeAbility: Encoder[Ability] = Encoder.instance { c =>
    {
      c match {
        case v: SwingWeaponAbility =>
          Map("SwingWeaponAbility" -> v).asJson
        case v: MeteorRainAbility =>
          Map("MeteorRainAbility" -> v).asJson
        case v: BubbleAbility =>
          Map("BubbleAbility" -> v).asJson
        case v: IceSpearAbility =>
          Map("IceSpearAbility" -> v).asJson
        case v: FistSlamAbility =>
          Map("FistSlamAbility" -> v).asJson
        case v: MeteorCrashAbility =>
          Map("MeteorCrashAbility" -> v).asJson
        case v: ThrustWeaponAbility =>
          Map("ThrustWeaponAbility" -> v).asJson
        case v: DashAbility =>
          Map("DashAbility" -> v).asJson
        case v: BowShotAbility =>
          Map("BowShotAbility" -> v).asJson
      }
    }
  }

  implicit val decodeAbility: Decoder[Ability] = Decoder.instance(c => {
    val fname = c.keys.flatMap(_.headOption).toSeq.head
    fname match {
      case "MeteorRainAbility"   => c.downField(fname).as[MeteorRainAbility]
      case "BubbleAbility"       => c.downField(fname).as[BubbleAbility]
      case "IceSpearAbility"     => c.downField(fname).as[IceSpearAbility]
      case "FistSlamAbility"     => c.downField(fname).as[FistSlamAbility]
      case "MeteorCrashAbility"  => c.downField(fname).as[MeteorCrashAbility]
      case "ThrustWeaponAbility" => c.downField(fname).as[ThrustWeaponAbility]
      case "SwingWeaponAbility"  => c.downField(fname).as[SwingWeaponAbility]
      case "DashAbility"         => c.downField(fname).as[DashAbility]
      case "BowShotAbility"         => c.downField(fname).as[BowShotAbility]
    }
  })
}
