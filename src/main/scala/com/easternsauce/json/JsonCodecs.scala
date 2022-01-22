package com.easternsauce.json

import com.easternsauce.model.area.Area
import com.easternsauce.model.creature._
import com.easternsauce.model.creature.ability.AbilityState.AbilityState
import com.easternsauce.model.creature.ability.ComponentType.ComponentType
import com.easternsauce.model.creature.ability._
import com.easternsauce.model.event.UpdateEvent
import com.easternsauce.model.item.{Item, ItemParameterValue, ItemTemplate}
import com.easternsauce.model.util.SimpleTimer
import com.easternsauce.model.{GameState, InventoryState}
import com.easternsauce.util.Direction.Direction
import com.easternsauce.util.{Direction, Vector2Wrapper}
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
  implicit val decodeInventoryState: Decoder[InventoryState] = deriveDecoder
  implicit val encodeInventoryState: Encoder[InventoryState] = deriveEncoder
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
  implicit val decodeVector2Wrapper: Decoder[Vector2Wrapper] = deriveDecoder
  implicit val encodeVector2Wrapper: Encoder[Vector2Wrapper] = deriveEncoder
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
  implicit val decodeRegularAttack: Decoder[RegularAttack] = deriveDecoder
  implicit val encodeRegularAttack: Encoder[RegularAttack] = deriveEncoder
  implicit val decodeTestProjectile: Decoder[TestProjectile] = deriveDecoder
  implicit val encodeTestProjectile: Encoder[TestProjectile] = deriveEncoder

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
        case v: RegularAttack =>
          Map("RegularAttack" -> v).asJson
        case v: TestProjectile =>
          Map("TestProjectile" -> v).asJson

      }
    }
  }

  implicit val decodeAbility: Decoder[Ability] = Decoder.instance(c => {
    val fname = c.keys.flatMap(_.headOption).toSeq.head
    fname match {
      case "RegularAttack"  => c.downField(fname).as[RegularAttack]
      case "TestProjectile" => c.downField(fname).as[TestProjectile]
    }
  })
}
