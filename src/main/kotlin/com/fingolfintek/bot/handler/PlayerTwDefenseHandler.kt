package com.fingolfintek.bot.handler

import com.fingolfintek.swgohgg.guild.GuildChannelRepository
import com.fingolfintek.swgohgg.player.PlayerCollection
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

          if (playerName.isEmpty())
            guildChannelRepository
                .getRosterForChannel(message.channel.id)
                .map {  }
          else
            processTeamsFor(playerName, message)
        })
        .onFailure { message.respondWith("Error processing message: ${it.message}") }
  }

  private fun processTeamsFor(playerName: String, message: Message) {
    val teams = resolveOptimalTeamsFor(playerName, guildChannelRepository
        .getRosterForChannel(message.channel.id))


    val teamsMessage = teams.joinToString("\n\n") { "$it" }
    message.respondWith("$playerName's teams\n\n$teamsMessage")
  }

  private fun resolveOptimalTeamsFor(
      playerName: String, map: Map<String, PlayerCollection>): List<Team> {

    val roster = map[playerName]!!

    val rosterUnitsByName = roster.units
        .toMap { Tuple.of(it.unit.name, it) }
        .toJavaMap()

    val compatibleTeams = teamDefinitions.tw.defense
        .compatibleTeamsFor(roster)

    return Stream
        .ofAll(Collections2.permutations(compatibleTeams.teams))
        .map { teams ->
          val tmpRoster = HashMap(rosterUnitsByName)

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
        .sortBy { it.size }
        .reverse()
        .head()
  }

}
