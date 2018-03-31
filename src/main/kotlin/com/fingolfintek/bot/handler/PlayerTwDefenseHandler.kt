package com.fingolfintek.bot.handler

import com.fingolfintek.swgohgg.guild.GuildChannelRepository
import com.fingolfintek.swgohgg.player.PlayerCollection
import com.fingolfintek.teams.OptimalTeamsResolver
import com.fingolfintek.teams.Team
import io.vavr.control.Try
import net.dv8tion.jda.core.entities.Message
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component
open class PlayerTwDefenseHandler(
    private val guildChannelRepository: GuildChannelRepository,
    private val teamsResolver: OptimalTeamsResolver
) : MessageHandler {

  private val messageRegex = Regex(
      "!tw\\s+(verbose\\s+)?defense\\s*(.+)",
      setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
  )

  override fun isApplicableTo(message: Message): Boolean =
      message.content.trim().matches(messageRegex)

  override fun processMessage(message: Message) {
    Try.ofSupplier { messageRegex.matchEntire(message.content)!! }
        .andThen(Consumer {
          val playerName = it.groupValues[2].trim()

          val guildRoster = guildChannelRepository
              .getRosterForChannel(message.channel.id)
              .toSortedMap()

          message.channel.sendTyping().queue()
          processTeamsFor(guildRoster[playerName]!!, it.groupValues[1].isNotBlank(), message)
        })
        .onFailure {
          message.respondWithEmbed("Territory War", "Error processing message: ${it.message}")
        }
  }

  private fun processTeamsFor(roster: PlayerCollection, verbose: Boolean, message: Message) {
    val compatibleTeams = teamsResolver.compatibleTeamsFor(roster)
    val optimalTeams = teamsResolver.resolveOptimalTeamsFor(compatibleTeams)

    if (verbose) {
      message.respondWithEmbed(
          "${roster.name}'s compatible teams",
          prettyPrint(compatibleTeams.teams))
    }

    message.respondWithEmbed(
        "${roster.name}'s optimal teams",
        prettyPrint(optimalTeams))
  }

  private fun prettyPrint(optimalTeams: List<Team>) =
      optimalTeams.joinToString("\n\n") { "$it" }
}
