package com.fingolfintek.teams

import com.fingolfintek.swgohgg.player.CollectedUnit
import io.vavr.Tuple
import io.vavr.collection.Stream
import io.vavr.control.Option
import org.apache.commons.collections4.CollectionUtils
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.Comparator.comparing

@Component
open class OptimalTeamsResolver {

  @Cacheable(cacheNames = ["teams"], key = "#compatibleTeams.sha1()")
  open fun resolveOptimalTeamsFor(compatibleTeams: PlayerTeamCollection): List<Team> {
    val teams = Stream.ofAll(compatibleTeams.teams)

    val roster = teams
        .flatMap { it.units }
        .toMap { Tuple.of(it.unit.name, it) }
        .toJavaMap()

    return teams
        .flatMap { generateRecursive(it, teams, roster) }
        .sorted(byTeamCountThenTotalPower())
        .headOption()
        .map { it.toJavaList() }
        .getOrElse { emptyList() }
  }

  private fun generateRecursive(
      start: Team, seedTeams: Stream<Team>, roster: Map<String, CollectedUnit>
  ): Stream<Stream<Team>> {

    val currentTeam = Option.of(start)
        .filter { it.supportedIn(roster) }

    val remainingRoster = currentTeam
        .map { team -> roster.filterKeys { !team.unitNames.contains(it) } }
        .getOrElse { roster }

    val newSeed = seedTeams.remove(start)
        .filter { it.supportedIn(remainingRoster) }

    return if (newSeed.isEmpty)
      currentTeam.map { Stream.of(Stream.of(it)) }.getOrElse { Stream.empty() }
    else newSeed.flatMap {
      val generated = generateRecursive(it, newSeed, remainingRoster)
      return@flatMap currentTeam
          .map { team -> generated.map { it.prepend(team) } }
          .getOrElse { generated }
    }
  }

  private fun Team.supportedIn(inventory: Map<String, CollectedUnit>): Boolean =
      CollectionUtils.containsAll(inventory.keys, unitNames)

  private fun byTeamCountThenTotalPower() =
      comparing<Stream<Team>, Int> { it.size() }.reversed()
          .thenByDescending { it.map { it.power() }.sum().toInt() }

}
