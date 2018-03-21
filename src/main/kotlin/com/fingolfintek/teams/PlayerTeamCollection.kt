package com.fingolfintek.teams

import com.fingolfintek.swgohgg.player.CollectedUnit
import java.io.Serializable

data class PlayerTeamCollection(
    val name: String,
    val teams: List<Team>
) : Serializable

data class Team(
    val name: String,
    val units: List<CollectedUnit>) {

  override fun toString(): String {
    return "$name\n${units.joinToString("\n\t", "\t") { it.unit.name }}"
  }
}
