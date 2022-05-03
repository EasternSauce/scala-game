package com.easternsauce.model.area

case class Area(
  areaId: String,
  creatures: List[String] = List(),
  spawnPoints: List[EnemySpawnPoint],
  params: AreaParams = AreaParams()
) {}
