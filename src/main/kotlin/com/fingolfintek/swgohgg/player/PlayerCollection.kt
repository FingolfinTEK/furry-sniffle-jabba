package com.fingolfintek.swgohgg.player

import io.vavr.collection.List
import io.vavr.collection.Map
import java.io.Serializable

data class PlayerCollection(
    val name: String,
    val units: List<CollectedUnit>
) : Serializable {

  fun withZetas(zetas: Map<String, Map<String, Set<String>>>): PlayerCollection {
    return copy(units = units
        .map { unit ->
          unit.copy(zetas = zetas[name]
              .map { it[unit.unit.name].getOrElse { emptySet() } }
              .getOrElse { emptySet() }
          )
        })
  }
}
