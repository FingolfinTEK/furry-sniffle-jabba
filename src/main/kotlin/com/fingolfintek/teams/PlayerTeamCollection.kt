package com.fingolfintek.teams

import com.fingolfintek.swgohgg.player.CollectedUnit
import java.io.Serializable

data class PlayerTeamCollection(
    val name: String,
    val teams: List<Team>
) : Serializable {

  fun withoutTeamsThatShareUnitsWith(pickedTeams: Iterable<Team>) =
      copy(teams = teams.filter { it.noUnitsSharedWith(pickedTeams) })
}

data class Team(
    val name: String,
    val units: List<CollectedUnit>)
  : Serializable {

  val unitNames: Set<String> = units.map { it.unit.name }.toSet()

  fun noUnitsSharedWith(teams: Iterable<Team>) =
      teams.flatMap { it.unitNames }.intersect(unitNames).isEmpty()

  fun power(): Int {
    return units.map { it.power }.sum()
  }

  override fun toString(): String {
    return "$name (${power() / 1000}k GP)\n${units.joinToString("\n\t", "\t") { it.unit.name }}"
  }
}
