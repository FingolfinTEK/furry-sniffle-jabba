package com.fingolfintek.teams

import com.google.common.collect.Collections2
import io.vavr.Tuple
import io.vavr.collection.Stream
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
open class OptimalTeamsResolver {

  @Cacheable(cacheNames = arrayOf("teams"), key = "#compatibleTeams.name")
  open fun resolveOptimalTeamsFor(compatibleTeams: PlayerTeamCollection): List<Team> {
    val compatibleUnitsByName = Stream
        .ofAll(compatibleTeams.teams)
        .flatMap { it.units }
        .toMap { Tuple.of(it.unit.name, it) }
        .toJavaMap()

    return Stream
        .ofAll(Collections2.permutations(compatibleTeams.teams))
        .map { teams ->
          val tmpRoster = HashMap(compatibleUnitsByName)

          teams.filter {
            val teamSupported = it.units
                .map { tmpRoster.contains(it.unit.name) }
                .reduce { acc, b -> acc && b }

            if (teamSupported) {
              it.units.forEach { tmpRoster.remove(it.unit.name) }
            }

            teamSupported
          }
        }
        .sorted(Comparator
            .comparing<List<Team>, Int> { it.size }
            .reversed()
            .thenByDescending { it.map { it.power() }.sum() }
        )
        .head()

  }
}
