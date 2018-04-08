package com.fingolfintek.teams

import io.vavr.Tuple2
import io.vavr.collection.*
import io.vavr.collection.Map
import io.vavr.collection.Set
import io.vavr.control.Option
import javax.annotation.PostConstruct

//@Component
//@DependsOn("teamsPostProcessor")
open class TeamsCombinationsResolver(
    private val teamDefinitions: Teams) {

  @PostConstruct
  open fun resolveOptimalTeamsFor() {
    val teams = HashMap.ofAll(teamDefinitions.templates)

    val roster = teams
        .values()
        .flatMap { it.characters.map { it.name } }
        .toSet()

    val possibleTeams = teams
        .flatMap { it -> generateRecursive(it, teams, roster) }

    println("Possible team combinations ${possibleTeams.size()}")
//          possibleTeams.forEach {
//            println("${it.mkString(",")}")
//          }
  }

  private fun generateRecursive(
      start: Tuple2<String, Teams.SquadTemplate>,
      seedTeams: Map<String, Teams.SquadTemplate>,
      roster: Set<String>
  ): Seq<Stream<String>> {

    val currentTeam = Option.of(start)
        .filter { it.supportedIn(roster) }

    val remainingRoster = currentTeam
        .map { team -> roster.removeAll(team._2.characters.map { it.name }) }
        .getOrElse { roster }

    val newSeed = seedTeams.remove(start._1)
        .filterValues { remainingRoster.containsAll(it.characters.map { it.name }) }

    return if (newSeed.isEmpty)
      currentTeam.map { Stream.of(Stream.of(it._1)) }.getOrElse { Stream.empty() }
    else newSeed.flatMap { it ->
      val generated = generateRecursive(it, newSeed, remainingRoster)
      return@flatMap currentTeam
          .map { team -> generated.map { it.prepend(team._1) } }
          .getOrElse { generated }
    }
  }

  private fun Tuple2<String, Teams.SquadTemplate>.supportedIn(roster: Set<String>): Boolean {
    return roster.containsAll(_2.characters.map { it.name })
  }

}
