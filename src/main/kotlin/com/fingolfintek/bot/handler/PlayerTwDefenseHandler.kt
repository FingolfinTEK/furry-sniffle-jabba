package com.fingolfintek.bot.handler

import com.fingolfintek.swgohgg.guild.GuildChannelRepository
import com.fingolfintek.swgohgg.player.PlayerCollection
import com.fingolfintek.teams.PlayerTeamCollection
import com.fingolfintek.teams.Team
import com.fingolfintek.teams.Teams
import com.google.common.collect.Collections2
import io.vavr.Tuple
import io.vavr.collection.Stream
import io.vavr.control.Try
import net.dv8tion.jda.core.entities.Message
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component
open class PlayerTwDefenseHandler(
    private val teamDefinitions: Teams,
    private val guildChannelRepository: GuildChannelRepository
) : MessageHandler {

  private val messageRegex = Regex(
      "!tw\\s+defense\\s*(.+)?",
      RegexOption.IGNORE_CASE
  )

  override fun isApplicableTo(message: Message): Boolean =
      message.content.matches(messageRegex)

  override fun processMessage(message: Message) {
    Try.ofSupplier { messageRegex.matchEntire(message.content)!! }
        .andThen(Consumer {
          val playerName = it.groupValues[1]

          val guildRoster = guildChannelRepository
              .getRosterForChannel(message.channel.id)

          if (playerName.isEmpty()) {
            guildRoster
                .map { }
          } else
            processTeamsFor(guildRoster[playerName]!!, message)
        })
        .onFailure { message.respondWith("Error processing message: ${it.message}") }
  }

  private fun processTeamsFor(roster: PlayerCollection, message: Message) {
    val compatibleTeams = compatibleTeamsFor(roster)
    val compatibleTeamsMessage = compatibleTeams.teams.joinToString("\n\n") { "$it" }
    message.respondWith("${roster.name}'s compatible teams\n\n$compatibleTeamsMessage")

    val optimalTeams = resolveOptimalTeamsFor(compatibleTeams)
    val teamsMessage = optimalTeams.joinToString("\n\n") { "$it" }
    message.respondWith("${roster.name}'s optimal teams\n\n$teamsMessage")
  }

  private fun compatibleTeamsFor(roster: PlayerCollection) =
      teamDefinitions.tw.defense.compatibleTeamsFor(roster)

  private fun resolveOptimalTeamsFor(compatibleTeams: PlayerTeamCollection): List<Team> {
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
