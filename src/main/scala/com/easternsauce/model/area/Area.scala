package com.easternsauce.model.area

import com.easternsauce.model.area.loot.LootPile

case class Area(
  areaId: String,
  creatures: List[String] = List(),
  spawnPoints: List[EnemySpawnPoint],
  areaParams: AreaParams = AreaParams(List(LootPile("lootPile1", 0, 0))) // TODO: loot pile testing
) {}
