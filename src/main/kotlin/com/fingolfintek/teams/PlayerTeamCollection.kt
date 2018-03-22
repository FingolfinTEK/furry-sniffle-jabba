package com.fingolfintek.teams

import com.fingolfintek.swgohgg.player.CollectedUnit
import com.google.common.hash.Hashing
import java.io.Serializable
import java.nio.charset.Charset

data class PlayerTeamCollection(
    val name: String,
    val teams: List<Team>
) : Serializable {
  fun sha1(): String {
    return Hashing.sha1()
        .hashString(
            "$name${teams.joinToString(",") { it.name }}",
            Charset.defaultCharset())
        .toString()
  }
}

data class Team(
    val name: String,
    val units: List<CollectedUnit>)
  : Serializable {

  fun power(): Int {
    return units.map { it.power }.sum()
  }

  override fun toString(): String {
    return "__$name (${power() / 1000}k GP)__\n${units.joinToString("\n\t", "\t") { it.unit.name }}"
  }
}
