package com.fingolfintek.teams

import com.fingolfintek.swgohgg.player.CollectedUnit
import com.fingolfintek.swgohgg.player.PlayerCollection
import com.fingolfintek.teams.Teams.SquadTemplateRequirements
import io.vavr.Tuple
import io.vavr.collection.Map
import io.vavr.collection.Stream
import io.vavr.control.Option
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component
import java.util.Comparator.comparing

@Component
@DependsOn("teamsPostProcessor")
open class OptimalTeamsResolver(
    private val teamDefinitions: Teams) {

  open fun compatibleTeamsFor(roster: PlayerCollection): PlayerTeamCollection {
    return compatibleTeamsFor(roster, 0)
  }

  private fun compatibleTeamsFor(roster: PlayerCollection, tier: Int = 0) =
      teamDefinitions.tw.defense.compatibleTeamsFor(roster, tier)

  private fun SquadTemplateRequirements.compatibleTeamsFor(
      collection: PlayerCollection, tier: Int = 0): PlayerTeamCollection {

    val unitsByName = collection.units
        .toMap { it -> Tuple.of(it.unit.name, it) }

    val teams = templates
        .filterValues { tier == 0 || it.tier == tier }
        .filterValues {
          it.hasMinTotalPower(unitsByName, defaultRequirements.minTotalPower)
              && it.isFulfilledBy(unitsByName)
        }
        .map {
          val units = it.value.characters.map { unitsByName[it.name].get() }
          return@map Team(it.key, units)
        }

    return PlayerTeamCollection(collection.name, teams)
  }

  @Cacheable(cacheNames = ["teams"], key = "#collection.sha1()")
  open fun resolveOptimalTeamsFor(collection: PlayerCollection): List<Team> {
    val pickedTeams = ArrayList<Team>()

    for (tier in 1..3) {
      pickedTeams += resolveOptimalTeamsFor(
          compatibleTeamsFor(collection, tier)
              .withoutTeamsThatShareUnitsWith(pickedTeams)
      )
    }

    return pickedTeams
  }

  private fun resolveOptimalTeamsFor(
      compatibleTeams: PlayerTeamCollection): List<Team> {

    val teams = Stream.ofAll(compatibleTeams.teams)

    val roster = teams
        .flatMap { it.units }
        .toMap { Tuple.of(it.unit.name, it) }

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
      inventory.keySet().containsAll(unitNames)

  private fun byTeamCountThenTotalPower() =
      comparing<Stream<Team>, Int> { it.size() }.reversed()
          .thenByDescending { it.map { it.power() }.sum().toInt() }

}
