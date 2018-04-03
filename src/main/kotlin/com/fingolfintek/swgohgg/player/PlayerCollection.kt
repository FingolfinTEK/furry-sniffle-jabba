package com.fingolfintek.swgohgg.player

import com.google.common.hash.Hashing
import io.vavr.collection.List
import io.vavr.collection.Map
import java.io.Serializable
import java.nio.charset.Charset

data class PlayerCollection(
    val name: String,
    val units: List<CollectedUnit>
) : Serializable {

  fun sha1(): String {
    return Hashing.sha1()
        .hashString(
            "$name${units.joinToString(",") { it.toString() }}",
            Charset.defaultCharset())
        .toString()
  }

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
